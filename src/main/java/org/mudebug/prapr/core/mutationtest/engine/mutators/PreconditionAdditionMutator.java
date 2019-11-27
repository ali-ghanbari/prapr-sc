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

import java.util.List;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum PreconditionAdditionMutator implements PraPRMethodMutatorFactory {
    PRECONDITION_ADDITION_MUTATOR;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo collectedClassInfo,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
            /* I am avoiding insertion of code before calling a super/this constructor*/
            return Commons.dummyMethodVisitor(methodVisitor);
        }
        return new PreconditionAdditionMethodVisitor(methodVisitor,
                methodInfo, collectedClassInfo, this, context);
    }


    @Override
    public MethodVisitor create(MutationContext mutationContext, MethodInfo methodInfo, MethodVisitor methodVisitor) {
        throw new UnsupportedOperationException();
    }

    @Override
    public String getGloballyUniqueId() {
        return this.getClass().getName();
    }

    @Override
    public String getName() {
        return name();
    }
}

class PreconditionAdditionMethodVisitor extends MethodVisitor {
    private final List<Integer> nullableParamIndices;

    private final Type mutatedMethodReturnType;

    private final String mutatedMethodName;

    private final MethodMutatorFactory variant;

    private final MutationContext context;

    public PreconditionAdditionMethodVisitor(final MethodVisitor methodVisitor,
                                             final MethodInfo methodInfo,
                                             final CollectedClassInfo cci,
                                             final MethodMutatorFactory variant,
                                             final MutationContext context) {
        super(Opcodes.ASM6, methodVisitor);
        this.mutatedMethodReturnType = Type.getReturnType(methodInfo.getMethodDescriptor());
        this.mutatedMethodName = methodInfo.getName();
        this.nullableParamIndices =
                cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).nullableParamIndices;
        this.context = context;
        this.variant = variant;
    }

    @Override
    public void visitCode() {
        super.visitCode();
        final int sz = this.nullableParamIndices.size();
        if (sz > 0) {
            String msg = String.format("nullity checks are added for %d nullable parameters of the method %s",
                    sz, this.mutatedMethodName);
            final MutationIdentifier newId;
            newId = this.context.registerMutation(this.variant, msg);
            if (this.context.shouldMutate(newId)) {
                for (final int paramIndex : this.nullableParamIndices) {
                    injectNullityCheck(paramIndex);
                }
            }
        }
    }

    private void injectNullityCheck(final int paramIndex) {
        super.visitVarInsn(Opcodes.ALOAD, paramIndex);
        final Label lEscape = new Label();
        super.visitJumpInsn(Opcodes.IFNONNULL, lEscape);
        Commons.injectReturnStmt(this.mv, this.mutatedMethodReturnType, null, null);
        super.visitLabel(lEscape);
    }
}
