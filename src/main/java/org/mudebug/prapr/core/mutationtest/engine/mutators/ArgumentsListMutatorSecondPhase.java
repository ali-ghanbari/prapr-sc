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
import org.pitest.reloc.asm.commons.LocalVariablesSorter;

import java.util.Iterator;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;

import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.NONE;
import static org.mudebug.prapr.core.mutationtest.engine.mutators.util.Preference.LOCAL;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum ArgumentsListMutatorSecondPhase implements PraPRMethodMutatorFactory {
    VARIANT_0(0, NONE, NONE, LOCAL, LOCAL, NONE),
    VARIANT_1(1, NONE, NONE, LOCAL, LOCAL, NONE);

    private final Preference[] pattern;

    private final int variantOrdinal;

    ArgumentsListMutatorSecondPhase(final int variantOrdinal,
                                    final Preference... pattern) {
        this.variantOrdinal = variantOrdinal;
        this.pattern = pattern;
    }

    public Preference[] getPattern() {
        return pattern;
    }

    public int getVariantOrdinal() {
        return variantOrdinal;
    }

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo collectedClassInfo,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        final ALM2ndPhaseMethodVisitor almv;
        almv = new ALM2ndPhaseMethodVisitor(methodVisitor, methodInfo,
                context, collectedClassInfo, this);
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
        return name();
    }
}

class ALM2ndPhaseMethodVisitor extends MethodVisitor {
    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final ArgumentsListMutatorSecondPhase variant;

    private final MethodInfo mutatedMethodInfo;

    private final ScopeTracker scopeTracker;

    private final MutationContext context;

    LocalVariablesSorter lvs;

    ALM2ndPhaseMethodVisitor(MethodVisitor methodVisitor,
                                    MethodInfo methodInfo,
                                    MutationContext context,
                                    CollectedClassInfo cci,
                                    ArgumentsListMutatorSecondPhase variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.variant = variant;
        this.context = context;
        this.scopeTracker = new ScopeTracker(cci.findMethod(methodInfo.getName(),
                methodInfo.getMethodDescriptor()).localsInfo);
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.mutatedMethodInfo = methodInfo;
    }

    private int getArgsCount() {
        return this.variant.getPattern().length;
    }

    @Override
    public void visitMethodInsn(int opcode, String owner, String name, String desc, boolean itf) {
        final Type[] args = Type.getArgumentTypes(desc);
        if (args.length == getArgsCount()) {
            final List<LocalVarInfo> lvis = new LinkedList<>();
            final List<FieldInfo> fis = new LinkedList<>();
            for (int i = 0; i < args.length; i++) {
                final String paramDesc = args[i].getDescriptor();
                final Preference preference = this.variant.getPattern()[i];
                final int ordinal = this.variant.getVariantOrdinal();
                switch (preference) {
                    case LOCAL:
                        final List<LocalVarInfo> visibleLocals =
                                this.scopeTracker.visibleLocals;
                        final LocalVarInfo lvi = Commons.pickLocalVariable(visibleLocals,
                                paramDesc, 0, ordinal);
                        if (lvi == null) { // couldn't find; shouldn't continue
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                            return;
                        }
                        lvis.add(lvi);
                        break;
                    case FIELD:
                        final boolean isStatic = this.mutatedMethodInfo.isStatic();
                        final FieldInfo fi = Commons.pickField(this.mutatedClassFieldsInfo,
                                paramDesc, 0, ordinal, isStatic);
                        if (fi == null) { // couldn't find; shouldn't continue
                            super.visitMethodInsn(opcode, owner, name, desc, itf);
                            return;
                        }
                        fis.add(fi);
                }
            }
            final String msg = String.format("replaced call to %s%s with a call to %s%s",
                    name,
                    desc,
                    name,
                    desc);
            final MutationIdentifier id = this.context.registerMutation(this.variant, msg);
            if (this.context.shouldMutate(id)) {
                final int[] tempLocals = Commons.createTempLocals(this.lvs, args);
                Commons.storeValues(this.mv, args, tempLocals);
                // preparing the stack
                final Iterator<LocalVarInfo> lvit = lvis.iterator();
                final Iterator<FieldInfo> fit = fis.iterator();
                for (int i = 0; i < args.length; i++) {
                    final Preference preference = this.variant.getPattern()[i];
                    final Type type = args[i];
                    switch (preference) {
                        case FIELD:
                            final FieldInfo fi = fit.next();
                            Commons.injectFieldValue(this.mv, 0, fi, type);
                            fit.remove();
                            break;
                        case LOCAL:
                            final LocalVarInfo lvi = lvit.next();
                            Commons.injectLocalValue(this.mv, lvi.index, type);
                            lvit.remove();
                            break;
                        case DEFVAL:
                            Commons.injectDefaultValue(this.mv, type);
                            break;
                        case NONE:
                            Commons.injectLocalValue(this.mv, tempLocals[i], type);
                    }
                }
            }
        }
        super.visitMethodInsn(opcode, owner, name, desc, itf);
    }

    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
}
