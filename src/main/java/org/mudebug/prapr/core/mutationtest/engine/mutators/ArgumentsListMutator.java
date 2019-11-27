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
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.FieldInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.LocalVarInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.PraPRMethodInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ScopeTracker;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

import java.util.List;
import java.util.Map;

/**
 * This mutator mutates a method call to a call to its overload.
 *
 * For each overload with N parameters, we have several options to
 * supply the arguments.
 *  * in the ideal situation, we need to try every possible cases:
 *      for each parameter try existing expression, def value, a field, or a local
 *      but this approach is inefficient as we need to produce 4^N mutations.
 *  * the other approach is to prioritize the arguments:
 *      for each parameter we try the following cases.
 *                   Existing
 *                      |
 *       +--------------+------------------+
 *       |              |                  |
 *    Default         Field(s)          Local(s)
 *                      |                  |
 *                  First local        First field
 *                      |                  |
 *                   Default            Default
 *
 *   This greedy algorithm can be viewed as an approximation of
 *   Damerau–Levenshtein edit distance algorithm.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum ArgumentsListMutator implements PraPRMethodMutatorFactory {
    ARGUMENT_LIST_MUTATOR_0(0, Preference.DEFVAL, -1),
    ARGUMENT_LIST_MUTATOR_1(1, Preference.DEFVAL, -1),
    ARGUMENT_LIST_MUTATOR_2(0, Preference.LOCAL, 0),
    ARGUMENT_LIST_MUTATOR_3(0, Preference.LOCAL, 1),
    ARGUMENT_LIST_MUTATOR_4(1, Preference.LOCAL, 0),
    ARGUMENT_LIST_MUTATOR_5(1, Preference.LOCAL, 1),
    ARGUMENT_LIST_MUTATOR_6(0, Preference.FIELD, 0),
    ARGUMENT_LIST_MUTATOR_7(0, Preference.FIELD, 1),
    ARGUMENT_LIST_MUTATOR_8(0, Preference.FIELD, 2),
    ARGUMENT_LIST_MUTATOR_9(1, Preference.FIELD, 0),
    ARGUMENT_LIST_MUTATOR_a(1, Preference.FIELD, 1),
    ARGUMENT_LIST_MUTATOR_b(1, Preference.FIELD, 2),
    ARGUMENT_LIST_MUTATOR_c(2, Preference.FIELD, 0),
    ARGUMENT_LIST_MUTATOR_d(2, Preference.FIELD, 1),
    ARGUMENT_LIST_MUTATOR_e(2, Preference.FIELD, 2),
    ARGUMENT_LIST_MUTATOR_f(2, Preference.FIELD, 3),
    ARGUMENT_LIST_MUTATOR_g(3, Preference.FIELD, 0),
    ARGUMENT_LIST_MUTATOR_h(3, Preference.FIELD, 1),
    ARGUMENT_LIST_MUTATOR_i(3, Preference.FIELD, 2),
    ARGUMENT_LIST_MUTATOR_j(3, Preference.FIELD, 3),
    ARGUMENT_LIST_MUTATOR_k(2, Preference.LOCAL, 0),
    ARGUMENT_LIST_MUTATOR_l(2, Preference.LOCAL, 1),
    ARGUMENT_LIST_MUTATOR_m(3, Preference.LOCAL, 0),
    ARGUMENT_LIST_MUTATOR_n(3, Preference.LOCAL, 1);

    private final Preference preference;

    private final int overloadIndex;

    private final int preferenceIndex;

    ArgumentsListMutator(final int overloadIndex,
                         final Preference preference,
                         final int preferenceIndex) {
        this.overloadIndex = overloadIndex;
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
        final PraPRMethodMutatorFactory variant = this;
        final ArgumentsListMutatorMethodVisitor almv = new ArgumentsListMutatorMethodVisitor(context,
                methodInfo, methodVisitor, cci, variant, cache, this.overloadIndex,
                this.preference, this.preferenceIndex);
        final int methodAccess = Commons.getMethodAccess(methodInfo);
        almv.lvs = new LocalVariablesSorter(methodAccess, methodInfo.getMethodDescriptor(), almv);
        return almv.lvs;
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

}

final class ArgumentsListMutatorMethodVisitor extends MethodVisitor {
    private final PraPRMethodMutatorFactory variant;

    private final MutationContext context;

    private final Preference preference;

    private final int overloadIndex;

    private final int preferenceIndex;

    private final Map<String, List<PraPRMethodInfo>> mutatedClassMethodsInfo;

    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final MethodInfo mutatedMethodInfo;

    private final ClassByteArraySource cache;

    private final ScopeTracker scopeTracker;

    protected LocalVariablesSorter lvs;

    private final ClassName owningClassName;

    ArgumentsListMutatorMethodVisitor(final MutationContext context,
                                      final MethodInfo methodInfo,
                                      final MethodVisitor methodVisitor,
                                      final CollectedClassInfo cci,
                                      final PraPRMethodMutatorFactory variant,
                                      final ClassByteArraySource cache,
                                      final int overloadIndex,
                                      final Preference preference,
                                      final int preferenceIndex) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.mutatedClassMethodsInfo = cci.methodsInfo;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.mutatedMethodInfo = methodInfo;
        this.cache = cache;
        this.variant = variant;
        this.scopeTracker = new ScopeTracker(cci.findMethod(methodInfo.getName(),
                methodInfo.getMethodDescriptor()).localsInfo);
        this.preference = preference;
        this.overloadIndex = overloadIndex;
        this.preferenceIndex = preferenceIndex;
        this.owningClassName = Commons.getOwningClassName(methodInfo);
    }

    private String pickOverload(final int opcode,
                                final String name,
                                final String excludedDesc,
                                final Map<String, List<PraPRMethodInfo>> methodsInfo) {
        int count = 0;
        for (Map.Entry<String, List<PraPRMethodInfo>> ent : methodsInfo.entrySet()) {
            final String desc = ent.getKey();
            if (!desc.equals(excludedDesc) && Type.getReturnType(desc).equals(Type.getReturnType(excludedDesc))) {
                final List<PraPRMethodInfo> smil = ent.getValue();
                for (PraPRMethodInfo smi : smil) {
                    if (smi.name.equals(name)) {
                        final ClassName mutatedClassName = this.owningClassName;
                        if (mutatedClassName.equals(smi.owningClassName) || smi.isPublic) {
                            if ((opcode == Opcodes.INVOKESTATIC) == smi.isStatic) {
                                if (count == this.overloadIndex) {
                                    return desc;
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

    private int pickLocalVariable(final String desc, final int index) {
        final LocalVarInfo lvi = Commons.pickLocalVariable(this.scopeTracker.visibleLocals,
                desc, 0, index);
        return lvi == null ? -1 : lvi.index;
    }

    private int firstMatch(final Type[] asis, final boolean[] used, final Type cat) {
        for (int i = 0; i < asis.length; i++) {
            if (asis[i].equals(cat) && !used[i]) {
                return i;
            }
        }
        return -1;
    }

    private FieldInfo pickField(final String desc, final int index) {
        if (this.mutatedMethodInfo.isConstructor()) {
            return null;
        }
        return Commons.pickField(this.mutatedClassFieldsInfo,
                desc, 0, index, this.mutatedMethodInfo.isStatic());
    }

    private void prepareStack(final Type[] asis, final int[] tempLocals, final Type[] tobe) {
        final boolean[] used = new boolean[asis.length];
        for (final Type cat : tobe) {
            /* first priority is to use existing argument */
            final int argIndex = firstMatch(asis, used, cat);
            final int localIndex;
            final FieldInfo fieldInfo;
            if (argIndex >= 0) {
                used[argIndex] = true;
                localIndex = tempLocals[argIndex];
                fieldInfo = null;
            } else {
                /* fall back to using a default value, field(s), or local(s)*/
                final String catDesc = cat.getDescriptor();
                if (this.preference == Preference.FIELD) {
                    /* prefer field(s) over first local and default value */
                    fieldInfo = pickField(catDesc, this.preferenceIndex); // pick n'th field
                    if (fieldInfo == null) { // n'th field not found
                        localIndex = pickLocalVariable(catDesc, 0); // pick first local
                    } else {
                        localIndex = -1;
                    }
                    /* at the end if localIndex < 0, we shall use the default value */
                } else if (this.preference == Preference.LOCAL) {
                    localIndex = pickLocalVariable(catDesc, this.preferenceIndex); // pick n'th local
                    if (localIndex < 0) { // n'th local not found
                        fieldInfo = pickField(catDesc, 0); // pick first field
                    } else {
                        fieldInfo = null;
                    }
                    /* here, we have localIndex < 0, and at the end if fieldInfo == null,
                     we shall use the default value */
                } else {
                    localIndex = -1;
                    fieldInfo = null;
                    /* we should use default value at the end */
                }
            }
            if (fieldInfo != null) {
                /* this means that no existing expression is chosen, and */
                /* either we have preferred fields over others or a local
                 * has not found and we wished to use the first field */
                Commons.injectFieldValue(this.mv, 0, fieldInfo, cat);
            } else {
                /* localIndex could be either the index of preferred local or
                 * the index of temp local holding the value of existing
                 * expression */
                if (localIndex < 0) {
                    Commons.injectDefaultValue(this.mv, cat);
                } else {
                    Commons.injectLocalValue(this.mv, localIndex, cat);
                }
            }
        }
    }

    @Override
    public void visitMethodInsn(int opcode, String ownerInternalName, String name, String desc, boolean itf) {
        final String descPrime;
        if (ownerInternalName.equals(this.owningClassName.asInternalName())) {
            descPrime = pickOverload(opcode, name, desc, this.mutatedClassMethodsInfo);
        } else {
            final CollectedClassInfo cci = ClassInfoCollector.collect(this.cache, ownerInternalName);
            descPrime = pickOverload(opcode, name, desc, cci.methodsInfo);
        }
        if (descPrime != null) {
            final String msg = String.format("replaced call to %s%s with a call to %s%s",
                    name,
                    desc,
                    name,
                    descPrime);
            final MutationIdentifier newId = this.context.registerMutation(this.variant, msg);
            if (this.context.shouldMutate(newId)) {
                final Type[] asis = Type.getArgumentTypes(desc);
                final int[] tempLocals = Commons.createTempLocals(this.lvs, asis);
                Commons.storeValues(this.mv, asis, tempLocals);
                /* now we have receiver in the stack, in case the call is not static */
                Type[] tobe = Type.getArgumentTypes(descPrime);
                prepareStack(asis, tempLocals, tobe);
                super.visitMethodInsn(opcode, ownerInternalName, name, descPrime, itf);
            } else {
                super.visitMethodInsn(opcode, ownerInternalName, name, desc, itf);
            }
        } else {
            super.visitMethodInsn(opcode, ownerInternalName, name, desc, itf);
        }
    }

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}