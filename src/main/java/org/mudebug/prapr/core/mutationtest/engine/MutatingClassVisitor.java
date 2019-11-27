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

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.objectweb.asm.Opcodes;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.functional.F;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MethodName;
import org.pitest.mutationtest.engine.gregor.AvoidAssertsMethodAdapter;
import org.pitest.mutationtest.engine.gregor.AvoidStringSwitchedMethodAdapter;
import org.pitest.mutationtest.engine.gregor.ClassInfo;
import org.pitest.mutationtest.engine.gregor.LineTrackingMethodVisitor;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.PraPRMethodMutationContext;
import org.pitest.mutationtest.engine.gregor.PraPRMutaterClassContext;
import org.pitest.mutationtest.engine.gregor.analysis.InstructionTrackingMethodVisitor;
import org.pitest.mutationtest.engine.gregor.blocks.BlockTrackingMethodDecorator;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.MethodVisitor;

import java.util.Collection;
import java.util.HashSet;
import java.util.Set;

/**
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
class MutatingClassVisitor extends ClassVisitor {
    private final F<MethodInfo, Boolean> filter;

    private final PraPRMutaterClassContext context;

    private final Set<MethodMutatorFactory> methodMutators;

    private final CollectedClassInfo collectedClassInfo;

    private final ClassByteArraySource cache;

    private final GlobalInfo classHierarchy;

    MutatingClassVisitor(final ClassVisitor delegateClassVisitor,
                         final PraPRMutaterClassContext context,
                         final F<MethodInfo, Boolean> filter,
                         final Collection<MethodMutatorFactory> mutators,
                         final CollectedClassInfo collectedClassInfo,
                         final ClassByteArraySource cache,
                         final GlobalInfo classHierarchy) {
        super(Opcodes.ASM6, delegateClassVisitor);
        this.context = context;
        this.filter = filter;
        this.methodMutators = new HashSet<>(mutators);
        this.collectedClassInfo = collectedClassInfo;
        this.cache = cache;
        this.classHierarchy = classHierarchy;
    }

    @Override
    public MethodVisitor visitMethod(final int access, final String methodName, final String methodDescriptor,
                                     final String signature, final String[] exceptions) {
        final PraPRMethodMutationContext methodContext = new PraPRMethodMutationContext(this.context,
                Location.location(ClassName.fromString(this.context.getClassInfo().getName()),
                        MethodName.fromString(methodName), methodDescriptor));

        final MethodVisitor methodVisitor =
                this.cv.visitMethod(access, methodName, methodDescriptor, signature, exceptions);

        final MethodInfo info = new MethodInfo().withOwner(this.context.getClassInfo())
                .withAccess(access)
                .withMethodName(methodName)
                .withMethodDescriptor(methodDescriptor);

        if (this.filter.apply(info)) {
            return this.visitMethodForMutation(methodContext, info, methodVisitor);
        } else {
            return methodVisitor;
        }

    }

    private MethodVisitor visitMethodForMutation(final PraPRMethodMutationContext methodContext,
                                                 final MethodInfo methodInfo,
                                                 final MethodVisitor methodVisitor) {
        MethodVisitor next = methodVisitor;
        for (final MethodMutatorFactory each : this.methodMutators) {
            next = getMethodVisitor(each, methodContext, methodInfo, next);
        }

        return new InstructionTrackingMethodVisitor(wrapWithDecorators(methodContext, wrapWithFilters(methodContext, next)), methodContext);
    }

    private MethodVisitor getMethodVisitor(final MethodMutatorFactory methodMutatorFactory,
                                           final PraPRMethodMutationContext methodContext,
                                           final MethodInfo methodInfo,
                                           final MethodVisitor next) {
        if (methodMutatorFactory instanceof PraPRMethodMutatorFactory) {
            return ((PraPRMethodMutatorFactory) methodMutatorFactory).create(methodContext, methodInfo,
                    next, this.collectedClassInfo, this.cache, this.classHierarchy);
        }
        return methodMutatorFactory.create(methodContext, methodInfo, next);
    }

    //-------------------------------CREDIT: copied from PIT's source code

    @Override
    public void visit(final int version, final int access, final String name, final String signature,
                      final String superName, final String[] interfaces) {
        super.visit(version, access, name, signature, superName, interfaces);
        this.context.registerClass(new ClassInfo(version, access, name, signature, superName, interfaces));
    }

    @Override
    public void visitSource(final String source, final String debug) {
        super.visitSource(source, debug);
        this.context.registerSourceFile(source);
    }

    private static MethodVisitor wrapWithDecorators(final PraPRMethodMutationContext methodContext,
                                                    final MethodVisitor mv) {
        return wrapWithBlockTracker(methodContext, wrapWithLineTracker(methodContext, mv));
    }

    private static MethodVisitor wrapWithBlockTracker(final PraPRMethodMutationContext methodContext,
                                                      final MethodVisitor mv) {
        return new BlockTrackingMethodDecorator(methodContext, mv);
    }

    private static MethodVisitor wrapWithLineTracker(final PraPRMethodMutationContext methodContext,
                                                     final MethodVisitor mv) {
        return new LineTrackingMethodVisitor(methodContext, mv);
    }

    private MethodVisitor wrapWithFilters(final PraPRMethodMutationContext methodContext,
                                          final MethodVisitor wrappedMethodVisitor) {
        return wrapWithStringSwitchFilter(methodContext, wrapWithAssertFilter(methodContext, wrappedMethodVisitor));
    }

    private static MethodVisitor wrapWithStringSwitchFilter(final PraPRMethodMutationContext methodContext,
                                                            final MethodVisitor wrappedMethodVisitor) {
        return new AvoidStringSwitchedMethodAdapter(methodContext, wrappedMethodVisitor);

    }

    private static MethodVisitor wrapWithAssertFilter(final PraPRMethodMutationContext methodContext,
                                                      final MethodVisitor wrappedMethodVisitor) {
        return new AvoidAssertsMethodAdapter(methodContext, wrappedMethodVisitor);
    }
}
