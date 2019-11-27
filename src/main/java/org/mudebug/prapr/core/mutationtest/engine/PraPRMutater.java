package org.mudebug.prapr.core.mutationtest.engine;

/*
 * #%L
 * prapr-plugin
 * %%
 * Copyright (C) 2018 - 2019 University of Texas at Dallas
 * %%
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 * 
 *      http://www.apache.org/licenses/LICENSE-2.0
 * 
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 * #L%
 */

import org.mudebug.prapr.core.SuspChecker;
import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.pitest.bytecode.FrameOptions;
import org.pitest.bytecode.NullVisitor;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.classinfo.ComputeClassWriter;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.MutableList;
import org.pitest.functional.Option;
import org.pitest.functional.predicate.Predicate;
import org.pitest.mutationtest.engine.Mutant;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.PraPRMutaterClassContext;
import org.pitest.reloc.asm.ClassReader;
import org.pitest.reloc.asm.ClassWriter;
import org.pitest.util.Log;

import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.pitest.functional.prelude.Prelude.and;
import static org.pitest.functional.prelude.Prelude.not;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPRMutater implements Mutater {
    private final Map<String, String> computeCache;

    private final Predicate<MethodInfo> filter;

    private final ClassByteArraySource byteSource;

    private final Set<MethodMutatorFactory> mutators;

    private final SuspChecker suspChecker;

    private final GlobalInfo classHierarchy;

    public PraPRMutater(final Predicate<MethodInfo> filter,
                        final Collection<MethodMutatorFactory> mutators,
                        final ClassByteArraySource byteSource,
                        final SuspChecker suspChecker,
                        final GlobalInfo classHierarchy) {
        this.filter = filter;
        this.mutators =  new HashSet<>(mutators);
        this.byteSource = byteSource;
        this.suspChecker = suspChecker;
        this.classHierarchy = classHierarchy;
        this.computeCache = new HashMap<>();
    }

    @Override
    public Mutant getMutation(MutationIdentifier id) {
        final PraPRMutaterClassContext context = new PraPRMutaterClassContext();
        context.setTargetMutation(Option.some(id));
        final Option<byte[]> bytes = this.byteSource.getBytes(id.getClassName().asJavaName());
        final ClassReader reader = new ClassReader(bytes.value());
        final ClassWriter w = new ComputeClassWriter(this.byteSource,
                this.computeCache, FrameOptions.pickFlags(bytes.value()));
        final CollectedClassInfo cci = ClassInfoCollector.collect(bytes.value());
        final MutatingClassVisitor mca = new MutatingClassVisitor(w, context,
                filterMethods(), FCollection.filter(this.mutators, isMutatorFor(id)),
                cci, this.byteSource, this.classHierarchy);
        reader.accept(mca, ClassReader.EXPAND_FRAMES);

        final List<MutationDetails> details = context.getMutationDetails(context
                .getTargetMutation().value());

        return new Mutant(details.get(0), w.toByteArray());
    }

    private static Predicate<MethodMutatorFactory> isMutatorFor(final MutationIdentifier id) {
        return new Predicate<MethodMutatorFactory>() {
            @Override
            public Boolean apply(final MethodMutatorFactory a) {
                return id.getMutator().equals(a.getGloballyUniqueId());
            }
        };
    }

    @Override
    public List<MutationDetails> findMutations(ClassName classToMutate) {
        if (!this.suspChecker.isHit(classToMutate.asJavaName())) {
            Log.getLogger().info(String.format("*** THE CLASS %s IS LEFT UNMUTATED.", classToMutate.asJavaName()));
            return new MutableList<>();
        }

        final PraPRMutaterClassContext context = new PraPRMutaterClassContext();
        context.setTargetMutation(Option.<MutationIdentifier>none());
        return this.byteSource.getBytes(classToMutate.asInternalName()).flatMap(findMutations(context));
    }

    private F<byte[], Iterable<MutationDetails>> findMutations(final PraPRMutaterClassContext context) {
        return new F<byte[], Iterable<MutationDetails>>() {
            @Override
            public Iterable<MutationDetails> apply(final byte[] bytes) {
                return findMutationsForBytes(context, bytes);
            }
        };
    }

    private Collection<MutationDetails> findMutationsForBytes(final PraPRMutaterClassContext context,
                                                              final byte[] classToMutate) {
        final CollectedClassInfo cci = ClassInfoCollector.collect(classToMutate);
        final ClassReader first = new ClassReader(classToMutate);
        final NullVisitor nv = new NullVisitor();
        final MutatingClassVisitor mca = new MutatingClassVisitor(nv, context, filterMethods(),
                this.mutators, cci, this.byteSource, this.classHierarchy);
        first.accept(mca, ClassReader.EXPAND_FRAMES);
        return FCollection.filter(context.getCollectedMutations(), effectiveMutationChecker());
    }

    private F<MutationDetails, Boolean> effectiveMutationChecker() {
        return new F<MutationDetails, Boolean>() {
            @Override
            public Boolean apply(MutationDetails mutationDetails) {
                return mutationDetails.getInstructionIndex() >= 0;
            }
        };
    }

    private static Predicate<MethodInfo> shouldMutate(final SuspChecker suspChecker) {
        return new Predicate<MethodInfo>() {
            @Override
            public Boolean apply(MethodInfo a) {
                final ClassName owningClassName = Commons.getOwningClassName(a);
                return suspChecker.isHit(owningClassName.asJavaName(), a.getName() + a.getMethodDescriptor());
            }
        };
    }

    //------------------------- CREDIT: copied from PIT's source code

    private Predicate<MethodInfo> filterMethods() {
        return and(this.filter,
                filterSyntheticMethods(),
                shouldMutate(this.suspChecker),
                not(isGeneratedEnumMethod()),
                not(isGroovyClass()));
    }

    private static F<MethodInfo, Boolean> isGroovyClass() {
        return new Predicate<MethodInfo>() {
            @Override
            public Boolean apply(final MethodInfo a) {
                return a.isInGroovyClass();
            }
        };
    }

    private static Predicate<MethodInfo> filterSyntheticMethods() {
        return new Predicate<MethodInfo>() {
            @Override
            public Boolean apply(final MethodInfo a) {
                // filter out synthetic methods,
                // except lambda$... methods, which contain code from lambda expressions
                return !a.isSynthetic() || a.getName().startsWith("lambda$");
            }
        };
    }

    private static Predicate<MethodInfo> isGeneratedEnumMethod() {
        return new Predicate<MethodInfo>() {
            @Override
            public Boolean apply(final MethodInfo a) {
                return a.isGeneratedEnumMethod();
            }
        };
    }
}
