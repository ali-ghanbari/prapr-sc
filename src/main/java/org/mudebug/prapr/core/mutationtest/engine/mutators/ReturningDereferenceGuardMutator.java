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
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.FieldInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.LocalVarInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ScopeTracker;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.DEFVAL;
import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.LOCAL;
import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.FIELD;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum ReturningDereferenceGuardMutator implements PraPRMethodMutatorFactory {
    RET_DEREFERENCE_GUARD_MUTATOR_0(DEFVAL, -1),
    RET_DEREFERENCE_GUARD_MUTATOR_1(LOCAL, 0),
    RET_DEREFERENCE_GUARD_MUTATOR_3(FIELD, 0);

    private final Preference preference;

    private final int preferenceIndex;
    
    ReturningDereferenceGuardMutator(final Preference preference, final int preferenceIndex) {
        this.preference = preference;
        this.preferenceIndex = preferenceIndex;
    }

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        if (methodInfo.isStaticInitializer()) {
            /* This mutator does not make much sense in class initializer
             * code, as returning from a class initializer is not legal.
             * Therefore, I am ignoring class initializers */
            return Commons.dummyMethodVisitor(methodVisitor);
        }
        final ReturningDereferenceGuardMutatorMethodVisitor rdgmmv = 
                new ReturningDereferenceGuardMutatorMethodVisitor(context, methodInfo,
                        methodVisitor, cci, this.preference, this.preferenceIndex, this);
        final int methodAccess = Commons.getMethodAccess(methodInfo);
        rdgmmv.lvs = new LocalVariablesSorter(methodAccess, methodInfo.getMethodDescriptor(), rdgmmv);
        return rdgmmv.lvs;
    }


    @Override
    public org.pitest.reloc.asm.MethodVisitor create(MutationContext mutationContext, MethodInfo methodInfo, org.pitest.reloc.asm.MethodVisitor methodVisitor) {
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

class ReturningDereferenceGuardMutatorMethodVisitor extends MethodVisitor {
    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final ReturningDereferenceGuardMutator variant;

    private final MutationContext context;

    private final MethodInfo mutatedMethodInfo;

    private final ScopeTracker scopeTracker;

    private final Preference preference;

    private final int preferenceIndex;

    private final Type mutatedMethodReturnType;

    LocalVariablesSorter lvs;

    ReturningDereferenceGuardMutatorMethodVisitor(final MutationContext context,
                                                  final MethodInfo methodInfo,
                                                  final MethodVisitor methodVisitor,
                                                  final CollectedClassInfo cci,
                                                  final Preference preference,
                                                  final int preferenceIndex,
                                                  final ReturningDereferenceGuardMutator variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.scopeTracker =
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.mutatedMethodInfo = methodInfo;
        this.preference = preference;
        this.preferenceIndex = preferenceIndex;
        this.mutatedMethodReturnType = Type.getReturnType(methodInfo.getMethodDescriptor());
    }

    private LocalVarInfo pickLocalVariable(String desc) {
        return Commons.pickLocalVariable(this.scopeTracker.visibleLocals, desc, 0, this.preferenceIndex);
    }

    private FieldInfo pickField(String desc) {
        if (this.mutatedMethodInfo.isConstructor()) {
            return null;
        }
        return Commons.pickField(this.mutatedClassFieldsInfo,
                desc, 0, this.preferenceIndex, this.mutatedMethodInfo.isStatic());
    }

    private String makeDesc(final LocalVarInfo lvi, final FieldInfo fi) {
        if (this.mutatedMethodInfo.isVoid()) {
            return "enclosing method";
        } else {
            if (lvi == null && fi == null) {
                return String.format("default value %s", Commons.defValString(this.mutatedMethodReturnType));
            } else if (lvi == null) {
                return String.format("field %s", fi.name);
            } else {
                return String.format("local %s", lvi.name);
            }
        }
    }

    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        if (opcode == Opcodes.GETFIELD) {
            final LocalVarInfo lvi;
            final FieldInfo fi;
            if (this.preference == Preference.DEFVAL || this.mutatedMethodInfo.isVoid()) {
                lvi = null;
                fi = null;
            } else {
                final String mutatedMethodReturnTypeDesc = this.mutatedMethodReturnType.getDescriptor();
                if (this.preference == Preference.LOCAL) {
                    lvi = pickLocalVariable(mutatedMethodReturnTypeDesc);
                    fi = null;
                } else { // some field
                    lvi = null;
                    fi = pickField(mutatedMethodReturnTypeDesc);
                }
                if (lvi == null && fi == null) {
                    /* we were supposed to find a local variable or some field but we have failed find */
                    super.visitFieldInsn(opcode, owner, name, desc);
                    return;
                }
            }
            final String msg = String.format("the access to %s is guarded returning %s", 
                    name, makeDesc(lvi, fi));
            final MutationIdentifier newId = context.registerMutation(this.variant, msg);
            if (context.shouldMutate(newId)) {
                super.visitInsn(Opcodes.DUP);
                final Label lEscape = new Label();
                super.visitJumpInsn(Opcodes.IFNONNULL, lEscape);
                Commons.injectReturnStmt(this.mv, this.mutatedMethodReturnType, lvi, fi);
                super.visitLabel(lEscape);
                super.visitFieldInsn(opcode, owner, name, desc);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}