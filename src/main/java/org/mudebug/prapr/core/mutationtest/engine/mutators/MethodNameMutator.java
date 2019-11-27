package org.mudebug.prapr.core.mutationtest.engine.mutators;

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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.PraPRMethodInfo;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum MethodNameMutator implements PraPRMethodMutatorFactory {
    METHOD_NAME_MUTATOR_0,
    METHOD_NAME_MUTATOR_1,
    METHOD_NAME_MUTATOR_2,
    METHOD_NAME_MUTATOR_3,
    METHOD_NAME_MUTATOR_4,
    METHOD_NAME_MUTATOR_5,
    METHOD_NAME_MUTATOR_6,
    METHOD_NAME_MUTATOR_7,
    METHOD_NAME_MUTATOR_8,
    METHOD_NAME_MUTATOR_9;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        final MethodNameMutator variant = this;
        return new MethodNameMutatorMethodVisitor(context, methodInfo,
                methodVisitor, cci, cache, variant);
    }


    @Override
    public MethodVisitor create(MutationContext mutationContext, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGloballyUniqueId() {
        return String.format("%s_%d", this.getClass().getName(), this.ordinal());
    }

    @Override
    public String getName() {
        return this.name();
    }

    public static Iterable<PraPRMethodMutatorFactory> getVariants() {
        return Arrays.<PraPRMethodMutatorFactory> asList(values());
    }
}

class MethodNameMutatorMethodVisitor extends MethodVisitor {
    private final MutationContext context;

    private final MethodNameMutator variant;

    private final Map<String, List<PraPRMethodInfo>> mutatedClassMethodsInfo;

    private final ClassName mutatedClassName;

    private final ClassByteArraySource cache;

    MethodNameMutatorMethodVisitor(final MutationContext context,
                                   final MethodInfo methodInfo,
                                   final MethodVisitor methodVisitor,
                                   final CollectedClassInfo cci,
                                   final ClassByteArraySource cache,
                                   final MethodNameMutator variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
        this.mutatedClassMethodsInfo = cci.methodsInfo;
        this.mutatedClassName = Commons.getOwningClassName(methodInfo);
        this.cache = cache;
    }
    
    private String pickMethodName(final int opcode,
                                  final String excludedName,
                                  final String desc,
                                  final Map<String, List<PraPRMethodInfo>> methodsInfo) {
        final List<PraPRMethodInfo> smil = methodsInfo.get(desc);
        if (smil != null) { // because of performance issues, we don't care about inherited elements
            int counter = 0;
            for (final PraPRMethodInfo smi : smil) {
                final String name = smi.name;
                if (!name.equals(excludedName) && !isInitializer(name)) {
                    if (this.mutatedClassName.equals(smi.owningClassName) || smi.isPublic) {
                        if ((opcode == Opcodes.INVOKESTATIC) == smi.isStatic) {
                            if (counter == this.variant.ordinal()) {
                                return name;
                            }
                            counter++;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isInitializer(String methodName) {
        return methodName.matches("<init>|<clinit>");
    }

    @Override
    public void visitMethodInsn(int opcode, String ownerInternalName, String name, String desc, boolean itf) {
        if (MethodInfo.isConstructor(name)) {
            super.visitMethodInsn(opcode, ownerInternalName, name, desc, itf);
        } else {
            final String namePrime;
            if (ownerInternalName.equals(this.mutatedClassName.asInternalName())) {
                namePrime = pickMethodName(opcode, name, desc, this.mutatedClassMethodsInfo);
            } else {
                final CollectedClassInfo cci = ClassInfoCollector.collect(this.cache, ownerInternalName);
                namePrime = pickMethodName(opcode, name, desc, cci.methodsInfo);
            }
            if (namePrime != null) {
                final String msg = String.format("replaced call to %s with a call to %s", name, namePrime);
                final MutationIdentifier newId = this.context.registerMutation(this.variant, msg);
                if (this.context.shouldMutate(newId)) {
                    super.visitMethodInsn(opcode, ownerInternalName, namePrime, desc, itf);
                } else {
                    super.visitMethodInsn(opcode, ownerInternalName, name, desc, itf);
                }
            } else {
                super.visitMethodInsn(opcode, ownerInternalName, name, desc, itf);
            }
        }
    }
}