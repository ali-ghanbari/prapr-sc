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
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
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
public enum NonVoidMethodCallGuardMutator implements PraPRMethodMutatorFactory {
    NON_VOID_METHOD_CALL_GUARD_MUTATOR_0(DEFVAL, -1),
    NON_VOID_METHOD_CALL_GUARD_MUTATOR_1(LOCAL, 0),
    NON_VOID_METHOD_CALL_GUARD_MUTATOR_2(LOCAL, 1),
    NON_VOID_METHOD_CALL_GUARD_MUTATOR_3(FIELD, 0),
    NON_VOID_METHOD_CALL_GUARD_MUTATOR_4(FIELD, 1);
    
    private final Preference preference;

    private final int preferenceIndex;

    NonVoidMethodCallGuardMutator(final Preference preference,
                                  final int preferenceIndex) {
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
        final NonVoidMethodCallGuardMutatorMethodVisitor nmvgmmv =
                new NonVoidMethodCallGuardMutatorMethodVisitor(context, methodInfo, methodVisitor,
                        cci, this.preference, this.preferenceIndex, this);
        final int methodAccess = Commons.getMethodAccess(methodInfo);
        nmvgmmv.lvs = new LocalVariablesSorter(methodAccess, methodInfo.getMethodDescriptor(), nmvgmmv);
        return nmvgmmv.lvs;
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

class NonVoidMethodCallGuardMutatorMethodVisitor extends MethodVisitor {
    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final MethodMutatorFactory variant;

    private final MutationContext context;

    private final ScopeTracker scopeTracker;

    private final MethodInfo mutatedMethodInfo;

    private final Preference preference;

    private final int preferenceIndex;

    LocalVariablesSorter lvs;

    NonVoidMethodCallGuardMutatorMethodVisitor(final MutationContext context,
                                               final MethodInfo methodInfo,
                                               final MethodVisitor methodVisitor,
                                               final CollectedClassInfo cci,
                                               final Preference preference,
                                               final int preferenceIndex,
                                               final NonVoidMethodCallGuardMutator variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.scopeTracker =
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.variant = variant;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.mutatedMethodInfo = methodInfo;
        this.preference = preference;
        this.preferenceIndex = preferenceIndex;
    }

    private LocalVarInfo pickLocalVariable(String desc) {
        return Commons.pickLocalVariable(this.scopeTracker.visibleLocals,
                desc, 0, this.preferenceIndex);
    }

    private FieldInfo pickField(String desc) {
        if (this.mutatedMethodInfo.isConstructor()) {
            return null;
        }
        return Commons.pickField(this.mutatedClassFieldsInfo,
                desc, 0, this.preferenceIndex, this.mutatedMethodInfo.isStatic());
    }
    
    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        /* note: constructors return void */
        if (Commons.isVirtualCall(opcode) && !MethodInfo.isVoid(desc)) {
            final Type calleeReturnType = Type.getReturnType(desc);
            final LocalVarInfo lvi;
            final FieldInfo fi;
            final Preference pref = this.preference;
            if (pref == Preference.DEFVAL) {
                lvi = null;
                fi = null;
            } else {
                if (pref == Preference.LOCAL) {
                    lvi = pickLocalVariable(desc);
                    fi = null;
                } else { // some field
                    lvi = null;
                    fi = pickField(desc);
                }
                if (lvi == null && fi == null) {
                    /* we were supposed to find a local variable or some field but we have failed find */
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
            }
            String msg = String.format("the call to %s::%s%s is guarded using ",
                    owner.replace('/',  '.'), name, desc);
            if (lvi == null && fi == null) {
                msg += String.format("default value %s", Commons.defValString(calleeReturnType));
            } else if (fi == null) {
                msg += String.format("local %s", lvi.name);
            } else {
                msg += String.format("field %s", fi.name);
            }
            final MutationIdentifier newId = this.context.registerMutation(variant, msg);
            if (this.context.shouldMutate(newId)) {
                final Type[] args = Type.getArgumentTypes(desc);
                int[] tempLocals = Commons.createTempLocals(this.lvs, args);
                Commons.storeValues(this.mv, args, tempLocals);
                super.visitInsn(Opcodes.DUP);
                final Label lEscape = new Label();
                super.visitJumpInsn(Opcodes.IFNONNULL, lEscape);
                super.visitInsn(Opcodes.POP); // we are not going to call the method anymore
                if (lvi == null && fi == null) {
                    Commons.injectDefaultValue(this.mv, calleeReturnType);
                } else if (fi == null) {
                    Commons.injectLocalValue(this.mv, lvi.index, calleeReturnType);
                } else {
                    Commons.injectFieldValue(this.mv, 0, fi, calleeReturnType);
                }
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

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}