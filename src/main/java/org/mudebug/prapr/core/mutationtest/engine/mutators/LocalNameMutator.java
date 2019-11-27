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

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.LocalVarInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ScopeTracker;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum LocalNameMutator implements PraPRMethodMutatorFactory {
    LOCAL_NAME_MUTATOR_0,
    LOCAL_NAME_MUTATOR_1,
    LOCAL_NAME_MUTATOR_2,
    LOCAL_NAME_MUTATOR_3,
    LOCAL_NAME_MUTATOR_4,
    LOCAL_NAME_MUTATOR_5;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        return new LocalNameMutatorMethodVisitor(context, methodInfo, methodVisitor, cci, this);
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

class LocalNameMutatorMethodVisitor extends MethodVisitor {
    private final MutationContext context;

    private final ScopeTracker scopeTracker;

    private final LocalNameMutator variant;
    
    LocalNameMutatorMethodVisitor(final MutationContext context,
                                  final MethodInfo methodInfo,
                                  final MethodVisitor methodVisitor,
                                  final CollectedClassInfo cci,
                                  final LocalNameMutator variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.scopeTracker = 
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.variant = variant;
    }
    
    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
    
    private LocalVarInfo pickLocalVariable(String desc, int excludedIndex) {
        int count = 0;
        for (LocalVarInfo lvi : this.scopeTracker.visibleLocals) {
            if (lvi.index != excludedIndex && lvi.typeDescriptor.equals(desc)) {
                if (this.variant.ordinal() == count) {
                    return lvi;
                }
                count++;
            }
        }
        return null;
    }
    
    private boolean isLoad(int opcode) {
        switch (opcode) {
        case Opcodes.ILOAD:
        case Opcodes.LLOAD:
        case Opcodes.FLOAD:
        case Opcodes.DLOAD:    
        case Opcodes.ALOAD:
            return true;
        }
        return false;
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        final LocalVarInfo thisLocal = this.scopeTracker.find(var);
        if (thisLocal == null) {
            /* This is because of such cases:
             * int temp = x + x;
             * in this case, "temp" is not in the rhs scope so JVM freely generates an "istore" instruction
             * without recording the beginning of the scope of "temp."
             * 
             * We don't even need to check if it is a "xstore" instruction. */
            super.visitVarInsn(opcode, var);
            return;
        }
        final LocalVarInfo otherLocal = pickLocalVariable(thisLocal.typeDescriptor, var);
        if (otherLocal != null) {
            final String msg = String.format("local %s is replaced by local %s to be %s", thisLocal.name,
                    otherLocal.name,
                    isLoad(opcode) ? "used" : "defined");
            final MutationIdentifier newId = context.registerMutation(this.variant, msg);
            if (context.shouldMutate(newId)) {
                super.visitVarInsn(opcode, otherLocal.index);
            } else {
                super.visitVarInsn(opcode, var);
            }
        } else {
            super.visitVarInsn(opcode, var);
        }
    }
    
}