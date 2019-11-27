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
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.ClassInfoCollector;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.CollectedClassInfo;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.Commons;
import org.mudebug.prapr.core.mutationtest.engine.mutators.util.FieldInfo;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassName;
import org.pitest.mutationtest.engine.MutationIdentifier;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MutationContext;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public enum FieldNameMutator implements PraPRMethodMutatorFactory {
    FIELD_NAME_MUTATOR_0,
    FIELD_NAME_MUTATOR_1,
    FIELD_NAME_MUTATOR_2,
    FIELD_NAME_MUTATOR_3,
    FIELD_NAME_MUTATOR_4,
    FIELD_NAME_MUTATOR_5;

    @Override
    public MethodVisitor create(final MutationContext context,
                                final MethodInfo methodInfo,
                                final MethodVisitor methodVisitor,
                                final CollectedClassInfo cci,
                                final ClassByteArraySource cache,
                                final GlobalInfo classHierarchy) {
        return new FieldNameMutatorMethodVisitor(context, methodVisitor, methodInfo, cci, this, cache);
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

final class FieldNameMutatorMethodVisitor extends MethodVisitor {
    private final MutationContext context;

    private final FieldNameMutator variant;

    private final Map<String, List<FieldInfo>> mutatedClassFieldsInfo;

    private final ClassName mutatedClassName;

    private final ClassByteArraySource cache;

    FieldNameMutatorMethodVisitor(final MutationContext context,
                                  final MethodVisitor methodVisitor,
                                  final MethodInfo methodInfo,
                                  final CollectedClassInfo cci,
                                  final FieldNameMutator variant,
                                  final ClassByteArraySource cache) {
        super(Opcodes.ASM6, methodVisitor);
        this.context = context;
        this.variant = variant;
        this.mutatedClassFieldsInfo = cci.fieldsInfo;
        this.mutatedClassName = Commons.getOwningClassName(methodInfo);
        this.cache = cache;
    }
    
    private String pickFieldName(int opcode,
            String excludedName,
            String desc,
            Map<String, List<FieldInfo>> fieldsInfo) {
        final List<FieldInfo> fil = fieldsInfo.get(desc);
        if (fil != null) { // because of performance issues, we don't care about inherited elements
            int counter = 0;
            for (FieldInfo fi : fil) {
                if (!fi.name.equals(excludedName)) {
                    if (this.mutatedClassName.equals(fi.owningClassName) || fi.isPublic) {
                        if (isStaticAccess(opcode) == fi.isStatic && (!isStore(opcode) || !fi.isFinal)) {
                            if (counter == this.variant.ordinal()) {
                                return fi.name;
                            }
                            counter++;
                        }
                    }
                }
            }
        }
        return null;
    }
    
    private boolean isStaticAccess(int opcode) {
        return opcode == Opcodes.GETSTATIC || opcode == Opcodes.PUTSTATIC;
    }
    
    private boolean isStore(int opcode) {
        return opcode == Opcodes.PUTFIELD || opcode == Opcodes.PUTSTATIC;
    }

    @Override
    public void visitFieldInsn(int opcode, String ownerInternalName, String name, String desc) {
        final String namePrime;
        // the field that is accessed is in the mutated class
        if (ownerInternalName.equals(this.mutatedClassName.asInternalName())) {
            namePrime = pickFieldName(opcode, name, desc, this.mutatedClassFieldsInfo);
        } else {
            final CollectedClassInfo cci = ClassInfoCollector.collect(this.cache, ownerInternalName);
            namePrime = pickFieldName(opcode, name, desc, cci.fieldsInfo);
        }
        if (namePrime != null) {
            final String msg = String.format("replaced access to %s with an access to %s", name, namePrime);
            final MutationIdentifier newId = context.registerMutation(variant, msg);
            if (context.shouldMutate(newId)) {
                super.visitFieldInsn(opcode, ownerInternalName, namePrime, desc);
            } else {
                super.visitFieldInsn(opcode, ownerInternalName, name, desc);
            }
        } else {
            super.visitFieldInsn(opcode, ownerInternalName, name, desc);
        }
    }
}