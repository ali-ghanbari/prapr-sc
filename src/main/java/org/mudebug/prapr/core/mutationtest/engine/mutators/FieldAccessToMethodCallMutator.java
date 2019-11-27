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
import org.mudebug.prapr.core.commons.ImmutablePair;
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
import org.pitest.reloc.asm.Type;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum FieldAccessToMethodCallMutator implements PraPRMethodMutatorFactory {
    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR_0,
    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR_1,
    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR_2,
    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR_3,
    FIELD_ACCESS_TO_METHOD_CALL_MUTATOR_4;

    @Override
    public MethodVisitor create(MutationContext context,
                                MethodInfo methodInfo,
                                MethodVisitor methodVisitor,
                                CollectedClassInfo cci,
                                ClassByteArraySource cache,
                                GlobalInfo classHierarchy) {
        return new FieldAccessToMethodCallMethodVisitor(context, methodInfo,
                methodVisitor, cci, cache, this, cache, cci.isInterface());
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

class FieldAccessToMethodCallMethodVisitor extends MethodVisitor {
    private final Map<String, List<PraPRMethodInfo>> mutatedClassMethodsInfo;

    private final FieldAccessToMethodCallMutator variant;

    private final MethodInfo mutatedMethodInfo;

    private final ClassByteArraySource cba;

    private final MutationContext context;

    private final boolean isInterface; // determines if the mutated class is an interface

    private final ClassName owningClassName;

    FieldAccessToMethodCallMethodVisitor(final MutationContext context,
                                         final MethodInfo methodInfo,
                                         final MethodVisitor methodVisitor,
                                         final CollectedClassInfo cci, ClassByteArraySource cache,
                                         final FieldAccessToMethodCallMutator variant,
                                         final ClassByteArraySource cba,
                                         final boolean itf) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
        this.mutatedMethodInfo = methodInfo;
        this.mutatedClassMethodsInfo = cci.methodsInfo;
        this.cba = cba;
        this.isInterface = itf;
        this.owningClassName = Commons.getOwningClassName(methodInfo);
    }

    private ImmutablePair<String, PraPRMethodInfo> pickGetterMethod(final Type returnType,
                                                                    final boolean staticGetter,
                                                                    final Map<String, List<PraPRMethodInfo>> methodsInfo) {
        int count = 0;
        for (Map.Entry<String, List<PraPRMethodInfo>> ent : methodsInfo.entrySet()) {
            final String desc = ent.getKey();
            if (Type.getReturnType(desc).equals(returnType) && Type.getArgumentTypes(desc).length == 0) {
                final List<PraPRMethodInfo> smil = ent.getValue();
                for (PraPRMethodInfo smi : smil) {
                    if (this.owningClassName.equals(smi.owningClassName) || smi.isPublic) {
                        if ((!mutatedMethodInfo.isStatic() || smi.isStatic) && (staticGetter == smi.isStatic)) {
                            if (count == variant.ordinal()) {
                                return new ImmutablePair<>(desc, smi);
                            }
                            count++;
                        }
                    }
                }
            }
        }
        return null;
    }

    private ImmutablePair<String, PraPRMethodInfo> pickSetterMethod(final Type paramType,
                                                                    final boolean staticSetter,
                                                                    final Map<String, List<PraPRMethodInfo>> methodsInfo) {
        int count = 0;
        for (Map.Entry<String, List<PraPRMethodInfo>> ent : methodsInfo.entrySet()) {
            final String desc = ent.getKey();
            final Type[] argTypes = Type.getArgumentTypes(desc);
            if (Type.getReturnType(desc).getSort() == Type.VOID
                    && argTypes.length == 1 && argTypes[0].equals(paramType)) {
                final List<PraPRMethodInfo> smil = ent.getValue();
                for (PraPRMethodInfo smi : smil) {
                    if (!smi.name.equals("<init>")) {
                        if (this.owningClassName.equals(smi.owningClassName) || smi.isPublic) {
                            if ((!mutatedMethodInfo.isStatic() || smi.isStatic)
                                    && (!staticSetter || smi.isStatic)) {
                                if (count == variant.ordinal()) {
                                    return new ImmutablePair<>(desc, smi);
                                }
                                count++;
                            }
                        }
                    }
                }
            }
        }
        return null;
    }

    private static boolean isStatic(final int opcode) {
        switch (opcode) {
        case Opcodes.GETSTATIC:
        case Opcodes.PUTSTATIC:
            return true;
        }
        return false;
    }

    private boolean reading(int opcode) {
        switch (opcode) {
        case Opcodes.GETFIELD:
        case Opcodes.GETSTATIC:
            return true;
        }
        return false;
    }

    @Override
    public void visitFieldInsn(int opcode, String ownerInternalName, String name, String desc) {
        final Type expectedRetType = Type.getType(desc);
        final ImmutablePair<String, PraPRMethodInfo> descSmi;
        final boolean itf;
        if (reading(opcode)) {
            if (ownerInternalName.equals(this.owningClassName.asInternalName())) {
                descSmi = pickGetterMethod(expectedRetType, isStatic(opcode), this.mutatedClassMethodsInfo);
                itf = this.isInterface;
            } else {
                final CollectedClassInfo cci = ClassInfoCollector.collect(cba, ownerInternalName);
                descSmi = pickGetterMethod(expectedRetType, isStatic(opcode), cci.methodsInfo);
                itf = cci.isInterface();
            }
        } else { // writing
            if (ownerInternalName.equals(this.owningClassName.asInternalName())) {
                descSmi = pickSetterMethod(expectedRetType, isStatic(opcode), this.mutatedClassMethodsInfo);
                itf = this.isInterface;
            } else {
                final CollectedClassInfo cci = ClassInfoCollector.collect(cba, ownerInternalName);
                descSmi = pickSetterMethod(expectedRetType, isStatic(opcode), cci.methodsInfo);
                itf = cci.isInterface();
            }
        }
        if (descSmi != null) {
            final String methodDesc = descSmi.getFirst();
            final PraPRMethodInfo smi = descSmi.getSecond();
            final String md = String.format("the access to field %s.%s is replaced by the call to %s::%s%s",
                    ownerInternalName.replace('/', '.'), name, smi.owningClassName.asJavaName(),
                    smi.name, methodDesc);
            final MutationIdentifier newId = context.registerMutation(this.variant, md);
            if (context.shouldMutate(newId)) {
                if (smi.isStatic) {
                    super.visitMethodInsn(Opcodes.INVOKESTATIC, smi.owningClassName.asInternalName(),
                            smi.name, methodDesc, itf);
                } else if (smi.isPublic || smi.isProtected) {
                    if (itf) {
                        super.visitMethodInsn(Opcodes.INVOKEINTERFACE, smi.owningClassName.asInternalName(),
                                smi.name, methodDesc, true);
                    } else {
                        super.visitMethodInsn(Opcodes.INVOKEVIRTUAL, smi.owningClassName.asInternalName(),
                                smi.name, methodDesc, false);
                    }
                } else if (smi.isPrivate) {
                    super.visitMethodInsn(Opcodes.INVOKESPECIAL, smi.owningClassName.asInternalName(),
                            smi.name, methodDesc, false);
                } else {
                    super.visitFieldInsn(opcode, ownerInternalName, name, desc);
                }
            }  else {
                super.visitFieldInsn(opcode, ownerInternalName, name, desc);
            }
        } else {
            super.visitFieldInsn(opcode, ownerInternalName, name, desc);
        }
    }

}
