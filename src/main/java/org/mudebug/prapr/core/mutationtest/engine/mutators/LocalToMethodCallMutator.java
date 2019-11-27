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

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.commons.ImmutablePair;
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.LocalVarInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.PraPRMethodInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ScopeTracker;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;

import java.util.Arrays;
import java.util.List;
import java.util.Map;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum LocalToMethodCallMutator implements PraPRMethodMutatorFactory {
    LOCAL_TO_METHOD_MUTATOR_0;

    @Override
    public MethodVisitor create(MutationContext context,
                                MethodInfo methodInfo,
                                MethodVisitor methodVisitor,
                                CollectedClassInfo collectedClassInfo,
                                ClassByteArraySource cache,
                                GlobalInfo classHierarchy) {
        if (methodInfo.isConstructor()) {
            return Commons.dummyMethodVisitor(methodVisitor);
        }
        return new LocalToMethCallMethodVisitor(this, context,
                methodVisitor, methodInfo, collectedClassInfo, cache);
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

class LocalToMethCallMethodVisitor extends MethodVisitor {
    private final LocalToMethodCallMutator variant;

    private final String mutatedClassInternalName;

    private final ScopeTracker scopeTracker;

    private final ClassByteArraySource cache;

    private final MutationContext context;

    private final boolean isInterface;

    private final boolean mustStatic;

    LocalToMethCallMethodVisitor(final LocalToMethodCallMutator variant,
                                 final MutationContext context,
                                 final MethodVisitor methodVisitor,
                                 final MethodInfo methodInfo,
                                 final CollectedClassInfo cci,
                                 final ClassByteArraySource cache) {
        super(Opcodes.ASM6, methodVisitor);
        this.variant = variant;
        this.context = context;
        this.scopeTracker =
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.cache = cache;
        this.isInterface = cci.isInterface();
        this.mutatedClassInternalName = Commons.getOwningClassName(methodInfo).asInternalName();
        this.mustStatic = methodInfo.isStatic();
    }

    private LocalVarInfo getLocal(final int index) {
        for (final LocalVarInfo lvi : this.scopeTracker.visibleLocals) {
            if (lvi.index == index) {
                return lvi;
            }
        }
        return null;
    }

    private ImmutablePair<String, PraPRMethodInfo> pickMethod(final Map<String, List<PraPRMethodInfo>> methodsInfo,
                                                              final Type returnType,
                                                              final boolean mustPublic) {
        int count = 0;
        for (final Map.Entry<String, List<PraPRMethodInfo>> ent : methodsInfo.entrySet()) {
            final String desc = ent.getKey();
            if (Type.getReturnType(desc).equals(returnType)) {
                for (final PraPRMethodInfo methodInfo : ent.getValue()) {
                    if (isUseless(methodInfo.name)) {
                        continue;
                    }
                    if (!mustPublic || methodInfo.isPublic) {
                        final Type[] argTypes = Type.getArgumentTypes(desc);
                        if (methodInfo.isStatic) {
                            // var --> static_method(var)
                            // the method should return a value
                            // whose type is equal to that of var
                            if (argTypes.length == 1
                                    && argTypes[0].equals(returnType)) {
                                if (count == this.variant.ordinal()) {
                                    return new ImmutablePair<>(desc, methodInfo);
                                }
                                count++;
                            }
                        } else if (!this.mustStatic) {
                            if (argTypes.length == 0) {
                                if (count == this.variant.ordinal()) {
                                    return new ImmutablePair<>(desc, methodInfo);
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

    private boolean isUseless(final String methodName) {
        return methodName.matches("(equals|hashCode|toString|clone)");
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (opcode == Opcodes.ALOAD) {
            final LocalVarInfo thisLocal = getLocal(var);
            if (thisLocal != null) {
                final Type localType = Type.getType(thisLocal.typeDescriptor);
                if (localType.getSort() != Type.OBJECT) {
                    super.visitVarInsn(opcode, var);
                    return;
                }
                final String varTypeInternalName = localType.getInternalName();
                final CollectedClassInfo cci;
                cci = ClassInfoCollector.collect(this.cache, varTypeInternalName);
                final boolean mustPublic;
                final boolean itf;
                if (this.mutatedClassInternalName.equals(varTypeInternalName)) {
                    mustPublic = false;
                    itf = this.isInterface;
                } else {
                    mustPublic = true;
                    itf = cci.isInterface();
                }
                final ImmutablePair<String, PraPRMethodInfo> smiPair;
                smiPair = pickMethod(cci.methodsInfo, localType, mustPublic);
                if (smiPair != null) {
                    final String methodDesc = smiPair.getFirst();
                    final PraPRMethodInfo methodInfo = smiPair.getSecond();
                    final String msg;
                    if (methodInfo.isStatic) {
                        msg = String.format("the access to the local %s is replaced a call %s(%s)",
                                thisLocal.name,
                                methodInfo.name,
                                thisLocal.name);
                    } else {
                        msg = String.format("the access to the local %s is replaced a call %s.%s()",
                                thisLocal.name,
                                thisLocal.name,
                                methodInfo.name);
                    }
                    final MutationIdentifier newId;
                    newId = context.registerMutation(this.variant, msg);
                    if (context.shouldMutate(newId)) {
                        final String ownerClassInternalName;
                        ownerClassInternalName = methodInfo.owningClassName.asInternalName();
                        /* current local will remain in the stack to be used as
                         either receiver or the argument */
                        super.visitVarInsn(opcode, var);
                        if (methodInfo.isStatic) {
                            super.visitMethodInsn(Opcodes.INVOKESTATIC,
                                    ownerClassInternalName,
                                    methodInfo.name, methodDesc, itf);
                        } else {
                            if (methodInfo.isPublic || methodInfo.isProtected) {
                                if (itf) {
                                    super.visitMethodInsn(Opcodes.INVOKEINTERFACE,
                                            ownerClassInternalName,
                                            methodInfo.name, methodDesc, true);
                                } else {
                                    super.visitMethodInsn(Opcodes.INVOKEVIRTUAL,
                                            ownerClassInternalName,
                                            methodInfo.name, methodDesc, false);
                                }
                            } else if (methodInfo.isPrivate) {
                                super.visitMethodInsn(Opcodes.INVOKESPECIAL,
                                        ownerClassInternalName,
                                        methodInfo.name, methodDesc, false);
                            } else {
                                super.visitVarInsn(opcode, var);
                            }
                        }
                    } else {
                        super.visitVarInsn(opcode, var);
                    }
                } else { /* we failed to locate supplant method */
                    super.visitVarInsn(opcode, var);
                }
            } else {
                super.visitVarInsn(opcode, var);
            }
        } else {
            super.visitVarInsn(opcode, var);
        }
    }

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}