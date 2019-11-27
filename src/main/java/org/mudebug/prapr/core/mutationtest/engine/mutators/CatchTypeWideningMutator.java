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
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;

import java.util.Arrays;
import java.util.Objects;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum CatchTypeWideningMutator implements PraPRMethodMutatorFactory {
    CATCH_TYPE_WIDENING_MUTATOR_0,
    CATCH_TYPE_WIDENING_MUTATOR_1,
    CATCH_TYPE_WIDENING_MUTATOR_2,
    CATCH_TYPE_WIDENING_MUTATOR_3;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo collectedClassInfo,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        return new CatchTypeWideningMethodVisitor(methodVisitor, this, context, cache);
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
        return name();
    }

    public static Iterable<PraPRMethodMutatorFactory> getVariants() {
        return Arrays.<PraPRMethodMutatorFactory> asList(values());
    }
}

class  CatchTypeWideningMethodVisitor extends MethodVisitor {
    private final CatchTypeWideningMutator variant;

    private final ClassByteArraySource cache;

    private final MutationContext context;

    private int count;

    private Label handler;

    private String desc;

    public CatchTypeWideningMethodVisitor(MethodVisitor methodVisitor,
                                          CatchTypeWideningMutator variant,
                                          MutationContext context,
                                          ClassByteArraySource cache) {
        super(Opcodes.ASM6, methodVisitor);
        this.variant = variant;
        this.context = context;
        this.count = 0;
        this.cache = cache;
    }

    @Override
    public void visitLabel(Label label) {
        if (Objects.equals(label, this.handler)) {
            this.context.registerMutation(this.variant, this.desc);
        }
        super.visitLabel(label);
    }

    @Override
    public void visitTryCatchBlock(Label start, Label end, Label handler, String type) {
        if (type == null) {
            super.visitTryCatchBlock(start, end, handler, null);
            return;
        }
        if (this.count == this.variant.ordinal()) {
            final String replacedType = Commons.getSupertype(this.cache, type);
            if (replacedType == null) {
                super.visitTryCatchBlock(start, end, handler, type);
                return;
            }
            this.desc = String.format("catch type %s is replaced with %s", type, replacedType);
            type = replacedType;
            this.handler = handler;
        }
        this.count++;
        super.visitTryCatchBlock(start, end, handler, type);
    }
}