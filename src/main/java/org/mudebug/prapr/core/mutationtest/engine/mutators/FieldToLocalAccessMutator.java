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
import java.util.HashSet;
import java.util.Set;

import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.PraPRMethodMutatorFactory;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
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
public enum FieldToLocalAccessMutator implements PraPRMethodMutatorFactory {
    FIELD_TO_LOCAL_ACCESS_MUTATOR_0,
    FIELD_TO_LOCAL_ACCESS_MUTATOR_1,
    FIELD_TO_LOCAL_ACCESS_MUTATOR_2;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        return new FieldToLocalAccessMutatorMethodVisitor(context,
                methodInfo, methodVisitor, cci, this, cache);
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

class FieldToLocalAccessMutatorMethodVisitor extends MethodVisitor {
    private final FieldToLocalAccessMutator variant;

    private final ClassByteArraySource cache;

    private final ScopeTracker scopeTracker;

    private final MutationContext context;

    FieldToLocalAccessMutatorMethodVisitor(final MutationContext context,
                                           final MethodInfo methodInfo,
                                           final MethodVisitor methodVisitor,
                                           final CollectedClassInfo cci,
                                           final FieldToLocalAccessMutator variant,
                                           final ClassByteArraySource cache) {
        super(Opcodes.ASM6, methodVisitor);
        this.variant = variant;
        this.scopeTracker = 
                new ScopeTracker(cci.findMethod(methodInfo.getName(), methodInfo.getMethodDescriptor()).localsInfo);
        this.context = context;
        this.cache = cache;
    }
    
    private LocalVarInfo pickLocalVariable(String desc) {
        int count = 0;
        for (LocalVarInfo lvi : this.scopeTracker.visibleLocals) {
            if (lvi.typeDescriptor.equals(desc)) {
                if (this.variant.ordinal() == count) {
                    return lvi;
                }
                count++;
            }
        }
        final Type desiredType = Type.getType(desc);
        if (desiredType.getSort() == Type.OBJECT) {
            count = 0;
            for (LocalVarInfo lvi : this.scopeTracker.visibleLocals) {
                final Type varType = Type.getType(lvi.typeDescriptor);
                if (varType.getSort() == Type.OBJECT) {
                    final String desiredTypeInternalName = desiredType.getInternalName();
                    final String varTypeInternalName = varType.getInternalName();
                    if (getSuperTypes(varTypeInternalName).contains(desiredTypeInternalName)) {
                        if (this.variant.ordinal() == count) {
                            return lvi;
                        }
                        count++;
                    }
                }
            }
        }
        return null;
    }

    private Set<String> getSuperTypes(String typeInternalName) {
        final Set<String> supers = new HashSet<>();
        while (typeInternalName != null) {
            typeInternalName = Commons.getSupertype(this.cache, typeInternalName);
            if (typeInternalName != null) {
                supers.add(typeInternalName);
            }
        }
        return supers;
    }
    
    @Override
    public void visitLabel(Label label) {
        this.scopeTracker.transfer(label);
        super.visitLabel(label);
    }
    
    private int varLoadOpcodeFor(Type type) {
        switch (type.getSort()) {
        case Type.INT:
        case Type.BOOLEAN:
        case Type.SHORT:
        case Type.BYTE:
        case Type.CHAR:
            return Opcodes.ILOAD;
        case Type.FLOAT:
            return Opcodes.FLOAD;
        case Type.LONG:
            return Opcodes.LLOAD;
        case Type.DOUBLE:
            return Opcodes.DLOAD;
        default:
            return Opcodes.ALOAD;
        }
    }
    
    private int varStoreOpcodeFor(Type type) {
        switch (type.getSort()) {
        case Type.INT:
        case Type.BOOLEAN:
        case Type.SHORT:
        case Type.BYTE:
        case Type.CHAR:
            return Opcodes.ISTORE;
        case Type.FLOAT:
            return Opcodes.FSTORE;
        case Type.LONG:
            return Opcodes.LSTORE;
        case Type.DOUBLE:
            return Opcodes.DSTORE;
        default:
            return Opcodes.ASTORE;
        }
    }
    
    @Override
    public void visitFieldInsn(int opcode, String owner, String name, String desc) {
        final LocalVarInfo localReplacement = pickLocalVariable(desc);
        if (localReplacement != null) {
            final String md = String.format("access to field %s is replaced by access to local %s",
                    name,
                    localReplacement.name);
            final MutationIdentifier newId = context.registerMutation(this.variant, md);
            if (context.shouldMutate(newId)) {
                final Type type = Type.getType(desc);
                final int opcodePrime;
                switch (opcode) {
                case Opcodes.GETFIELD:
                    super.visitInsn(Opcodes.POP);
                case Opcodes.GETSTATIC:
                    opcodePrime = varLoadOpcodeFor(type);
                    break;
                case Opcodes.PUTFIELD:
                    if (type.getSize() == 1) {
                        super.visitInsn(Opcodes.SWAP);
                    } else if (type.getSize() == 2) {
                        super.visitInsn(Opcodes.DUP2_X1); // swapping a two-worded value with a single-worded one  
                        super.visitInsn(Opcodes.POP2);
                    } else {
                        throw new IllegalArgumentException();
                    }
                    super.visitInsn(Opcodes.POP);
                case Opcodes.PUTSTATIC:
                    opcodePrime = varStoreOpcodeFor(type);
                    break;
                default:
                    throw new RuntimeException("unexpected opcode");
                }
                super.visitVarInsn(opcodePrime, localReplacement.index);
            } else {
                super.visitFieldInsn(opcode, owner, name, desc);
            }
        } else {
            super.visitFieldInsn(opcode, owner, name, desc);
        }
    }

}