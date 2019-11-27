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
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ScopeTracker;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum LocalToFieldAccessMutator implements PraPRMethodMutatorFactory {
    LOCAL_TO_FIELD_ACCESS_MUTATOR_0,
    LOCAL_TO_FIELD_ACCESS_MUTATOR_1,
    LOCAL_TO_FIELD_ACCESS_MUTATOR_2,
    LOCAL_TO_FIELD_ACCESS_MUTATOR_3,
    LOCAL_TO_FIELD_ACCESS_MUTATOR_4;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        if (methodInfo.isConstructor() || methodInfo.isStaticInitializer()) {
            /* I am avoiding insertion of code before call to super constructor.
             * Besides that, this mutator does not make much sense in constructor code,
             * so I am ignoring class initializers, as well */
            return Commons.dummyMethodVisitor(methodVisitor);
        }
        return new LocalToFieldAccessMutatorMethodVisitor(context, methodInfo, methodVisitor, cci, this);
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

class LocalToFieldAccessMutatorMethodVisitor extends MethodVisitor {
    private final MutationContext context;

    private final LocalToFieldAccessMutator variant;

    private final ScopeTracker scopeTracker;

    private final MethodInfo mutatedMethodInfo;

    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    LocalToFieldAccessMutatorMethodVisitor(final MutationContext context,
                                           final MethodInfo methodInfo,
                                           final MethodVisitor methodVisitor,
                                           final CollectedClassInfo cci,
                                           final LocalToFieldAccessMutator variant) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
        this.scopeTracker =
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.mutatedMethodInfo = methodInfo;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
    }
    
    private FieldInfo pickField(String desc, boolean toBeWrittenOn) {
        final List<FieldInfo> fil = this.mutatedClassFieldsInfo.get(desc);
        if (fil != null) { // because of performance issues, we don't care about inherited elements
            int counter = 0;
            for (FieldInfo fi : fil) {
                if ((!mutatedMethodInfo.isStatic() || fi.isStatic) && (!toBeWrittenOn || !fi.isFinal)) {
                    if (counter == this.variant.ordinal()) {
                        return fi;
                    }
                    counter++;
                }
            }
        }
        return null;
    }
    
    private boolean isStore(int opcode) {
        switch (opcode) {
        case Opcodes.ISTORE:
        case Opcodes.LSTORE:
        case Opcodes.FSTORE:
        case Opcodes.DSTORE:
        case Opcodes.ASTORE:
            return true;
        }
        return false;
    }
    
    private void loadThis() {
        super.visitVarInsn(Opcodes.ALOAD, 0);
    }
    
    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }

    @Override
    public void visitVarInsn(int opcode, int var) {
        if (opcode == Opcodes.RET) {
            super.visitVarInsn(opcode, var);
            return;
        }
        final LocalVarInfo thisLocal = this.scopeTracker.find(var);
        if (thisLocal != null) {
            final String desc = thisLocal.typeDescriptor;
            final FieldInfo fieldReplacement = pickField(desc, isStore(opcode));
            if (fieldReplacement != null) {
                final String md = String.format("access to local %s is replaced by access to field %s",
                        thisLocal.name,
                        fieldReplacement.name);
                final MutationIdentifier newId = context.registerMutation(this.variant, md);
                if (context.shouldMutate(newId)) {
                    final String fieldOwnerInternalName = fieldReplacement.owningClassName.asInternalName();
                    final String fieldName = fieldReplacement.name;
                    final int fieldAccessOpcode;
                    if (isStore(opcode)) {
                        if (fieldReplacement.isStatic) {
                            fieldAccessOpcode = Opcodes.PUTSTATIC;
                        } else {
                            /* note that if an instance field is chosen as replacement then we are
                             * sure that the mutated method is not static */
                            loadThis();
                            switch (Type.getType(desc).getSize()) {
                            case 1: // the value to be stored is int, short, float, etc.
                                super.visitInsn(Opcodes.SWAP);
                                break;
                            case 2: // the value to be store is long or double
                                super.visitInsn(Opcodes.DUP_X2);
                                super.visitInsn(Opcodes.POP);
                                break;
                            default: // just to exhaust all the cases
                                throw new RuntimeException();
                            }
                            fieldAccessOpcode = Opcodes.PUTFIELD;
                        }
                    } else {
                        if (fieldReplacement.isStatic) {
                            fieldAccessOpcode = Opcodes.GETSTATIC;
                        } else {
                            loadThis();
                            fieldAccessOpcode = Opcodes.GETFIELD;
                        }
                    }
                    super.visitFieldInsn(fieldAccessOpcode, fieldOwnerInternalName, fieldName, desc);
                } else {
                    super.visitVarInsn(opcode, var);
                }
            } else {
                super.visitVarInsn(opcode, var);
            }
        } else {
            /* Please refer to the description of Local Name Mutator to find out why
             * we are simply ignoring these cases */
            super.visitVarInsn(opcode, var);
        }
    }
}