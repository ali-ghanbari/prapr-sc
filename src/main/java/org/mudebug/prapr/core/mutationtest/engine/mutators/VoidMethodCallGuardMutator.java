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
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum VoidMethodCallGuardMutator implements PraPRMethodMutatorFactory {
    VOID_METHOD_CALL_GUARD_MUTATOR;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        final VoidMethodCallGuardMutatorMethodVisitor vmcgmmv = 
                new VoidMethodCallGuardMutatorMethodVisitor(context, methodVisitor, this);
        final int methodAccess = Commons.getMethodAccess(methodInfo);
        vmcgmmv.lvs = new LocalVariablesSorter(methodAccess, methodInfo.getMethodDescriptor(), vmcgmmv);
        return vmcgmmv.lvs;
    }

    @Override
    public org.pitest.reloc.asm.MethodVisitor create(MutationContext mutationContext, MethodInfo methodInfo, org.pitest.reloc.asm.MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getClass().getName();
    }

    @Override
    public String getName() {
        return this.name();
    }
}

class VoidMethodCallGuardMutatorMethodVisitor extends MethodVisitor {
    private final MethodMutatorFactory variant;

    private final MutationContext context;

    LocalVariablesSorter lvs;

    VoidMethodCallGuardMutatorMethodVisitor(final MutationContext context,
                                            final MethodVisitor methodVisitor,
                                            final MethodMutatorFactory variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        final Type calleeReturnType = Type.getReturnType(desc);
        final int calleeReturnSort = calleeReturnType.getSort();
        if (calleeReturnSort == Type.VOID && Commons.isVirtualCall(opcode) && !name.equals("<init>")) {
            final String msg = String.format("the call to %s::%s%s is guarded", owner.replace('/',  '.'), name, desc);
            final MutationIdentifier newId = this.context.registerMutation(this.variant, msg);
            if (this.context.shouldMutate(newId)) {
                final Type[] args = Type.getArgumentTypes(desc);
                int[] tempLocals = Commons.createTempLocals(this.lvs, args);
                Commons.storeValues(this.mv, args, tempLocals);
                super.visitInsn(Opcodes.DUP);
                final Label lEscape = new Label();
                super.visitJumpInsn(Opcodes.IFNONNULL, lEscape);
                super.visitInsn(Opcodes.POP); // we are not going to call the method anymore
                final Label lEnd = new Label();
                super.visitJumpInsn(Opcodes.GOTO, lEnd);
                super.visitLabel(lEscape);
                Commons.restoreValues(this.mv, tempLocals, args);
                super.visitMethodInsn(opcode, owner, name, desc, itf);
                super.visitLabel(lEnd);
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }
}