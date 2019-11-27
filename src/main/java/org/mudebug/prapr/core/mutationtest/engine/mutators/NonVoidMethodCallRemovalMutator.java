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

import java.util.Arrays;
import java.util.List;
import java.util.Map;

import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.DEFVAL;
import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.LOCAL;
import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.FIELD;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum NonVoidMethodCallRemovalMutator implements PraPRMethodMutatorFactory {
    NON_VOID_METHOD_CALL_MUTATOR_0(DEFVAL, -1),
    NON_VOID_METHOD_CALL_MUTATOR_1(LOCAL, 0),
    NON_VOID_METHOD_CALL_MUTATOR_3(FIELD, 0);

    private final Preference preference;

    private final int preferenceIndex;

    NonVoidMethodCallRemovalMutator(final Preference preference,
                                    final int preferenceIndex) {
        this.preference = preference;
        this.preferenceIndex = preferenceIndex;
    }

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo collectedClassInfo,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        return new NonVoidMethCallRemovalMethodVisitor(context, methodInfo, methodVisitor,
                this, this.preference, this.preferenceIndex, collectedClassInfo);
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
        return name();
    }

    public static Iterable<PraPRMethodMutatorFactory> getVariants() {
        return Arrays.<PraPRMethodMutatorFactory> asList(values());
    }
}

class NonVoidMethCallRemovalMethodVisitor extends MethodVisitor {
    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final NonVoidMethodCallRemovalMutator variant;

    private final ScopeTracker scopeTracker;

    private final MutationContext context;

    private final MethodInfo mutatedMethodInfo;

    private final Preference preference;

    private final int preferenceIndex;

    NonVoidMethCallRemovalMethodVisitor(final MutationContext context,
                                        final MethodInfo methodInfo,
                                        final MethodVisitor methodVisitor,
                                        final NonVoidMethodCallRemovalMutator variant,
                                        final Preference preference,
                                        final int preferenceIndex,
                                        final CollectedClassInfo cci) {
        super(Opcodes.ASM6, methodVisitor);
        this.variant = variant;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.scopeTracker =
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.context = context;
        this.mutatedMethodInfo = methodInfo;
        this.preference = preference;
        this.preferenceIndex = preferenceIndex;
    }

    private LocalVarInfo pickLocalVariable(final String desc, final int index) {
        return Commons.pickLocalVariable(this.scopeTracker.visibleLocals,
                desc, 0, index);
    }

    private FieldInfo pickField(final String desc, final int index) {
        if (this.mutatedMethodInfo.isConstructor()) {
            return null;
        }
        return Commons.pickField(this.mutatedClassFieldsInfo,
                desc, 0, index, this.mutatedMethodInfo.isStatic());
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        if (!MethodInfo.isVoid(desc)) { // mutate only non-void method calls. therefore, we don't mutate ctor calls
            final Type returnType = Type.getReturnType(desc);
            final LocalVarInfo lvi;
            final FieldInfo fi;
            if (this.preference == Preference.DEFVAL) {
                lvi = null;
                fi = null;
            } else {
                if (this.preference == Preference.LOCAL) {
                    lvi = pickLocalVariable(returnType.getDescriptor(), this.preferenceIndex);
                    fi = null;
                } else { // this.preference == Preference.FIELD
                    lvi = null;
                    fi = pickField(returnType.getDescriptor(), this.preferenceIndex);
                }
                if (lvi == null && fi == null) {
                    /* we were supposed to find a local variable or some field but we have failed find */
                    super.visitMethodInsn(opcode, owner, name, desc, itf);
                    return;
                }
            }
            String description = String.format("the call to %s::%s%s is replaced with the used of ",
                    owner.replace('/',  '.'), name, desc);
            if (lvi == null && fi == null) {
                description += String.format("default value %s", Commons.defValString(returnType));
            } else if (fi == null) {
                description += String.format("local %s", lvi.name);
            } else {
                description += String.format("field %s", fi.name);
            }
            final MutationIdentifier newId = this.context.registerMutation(variant, description);
            if (this.context.shouldMutate(newId)) {
                popArguments(desc);
                /* constructors return void, so we don't need worry about the
                 newly created object reference. */
                if (!Commons.isStaticCall(opcode)) {
                    super.visitInsn(Opcodes.POP); // pop receiver object
                }
                if (lvi == null && fi == null) {
                    Commons.injectDefaultValue(this.mv, returnType);
                } else if (fi == null) {
                    Commons.injectLocalValue(this.mv, lvi.index, returnType);
                } else {
                    Commons.injectFieldValue(this.mv, 0, fi, returnType);
                }
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        } else {
            super.visitMethodInsn(opcode, owner, name, desc, itf);
        }
    }

    private void popArguments(final String methodDesc) {
        final Type[] args = Type.getArgumentTypes(methodDesc);
        for (int i = args.length - 1; i >= 0; i--) {
            final Type argType = args[i];
            if (argType.getSize() == 1) {
                super.visitInsn(Opcodes.POP);
            } else {
                super.visitInsn(Opcodes.POP2);
            }
        }
    }

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}
