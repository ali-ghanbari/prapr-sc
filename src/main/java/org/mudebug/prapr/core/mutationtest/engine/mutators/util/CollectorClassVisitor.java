package org.mudebug.prapr.core.mutationtest.engine.mutators.util;

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

import java.util.ArrayList;
import java.util.LinkedList;
import java.util.List;

import org.pitest.classinfo.ClassName;
import org.pitest.reloc.asm.ClassVisitor;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.FieldVisitor;
import org.pitest.reloc.asm.Opcodes;
import org.pitest.reloc.asm.Type;

/**
 * The class visitor to collect information about classes to be mutated.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
class CollectorClassVisitor extends ClassVisitor {
    private final CollectedClassInfo cci;

    private ClassName owningClassName;

    CollectorClassVisitor() {
        super(Opcodes.ASM6);
        this.cci = new CollectedClassInfo();
    }
    
    public CollectedClassInfo getCollectedClassInfo() {
        return this.cci;
    }
    
    @Override
    public void visit(int version, int access, String name, String signature, String superName, String[] interfaces) {
        this.cci.setInterface((access & Opcodes.ACC_INTERFACE) != 0);
        this.owningClassName = ClassName.fromString(name);
        super.visit(version, access, name, signature, superName, interfaces);
    }

    @Override
    public FieldVisitor visitField(int access, String name, String desc, String signature, Object value) {
        List<FieldInfo> infoList = this.cci.fieldsInfo.get(desc);
        if (infoList == null) {
            infoList = new ArrayList<>();
            this.cci.fieldsInfo.put(desc, infoList);
        }
        infoList.add(new FieldInfo(access, name, this.owningClassName));
        return super.visitField(access, name, desc, signature, value);
    }

    @Override
    public MethodVisitor visitMethod(int access, String name, String desc, String signature, String[] exceptions) {
        final List<Integer> nullableParams = new LinkedList<>();
        int argIndex = 0;
        if ((access & Opcodes.ACC_STATIC) == 0) {
            argIndex++; // this can never be null
        }
        final Type[] argTypes = Type.getArgumentTypes(desc);
        for (final Type at : argTypes) {
            switch (at.getSort()) {
                case Type.ARRAY:
                case Type.OBJECT:
                case Type.METHOD:
                    nullableParams.add(argIndex);
            }
            argIndex += at.getSize();
        }

        List<PraPRMethodInfo> infoList = this.cci.methodsInfo.get(desc);
        if (infoList == null) {
            infoList = new ArrayList<>();
            this.cci.methodsInfo.put(desc, infoList);
        }
        final MethodVisitor superMethodVisitor = super.visitMethod(access, name, desc, signature, exceptions);
        final CollectorMethodVisitor cmv = new CollectorMethodVisitor(superMethodVisitor);
        /*note: CollectorMethodVisitor::localsInfo is going to be populated later*/
        infoList.add(new PraPRMethodInfo(access, name, cmv.getLocalsInfo(), this.owningClassName, nullableParams));
        return cmv;
    }
}