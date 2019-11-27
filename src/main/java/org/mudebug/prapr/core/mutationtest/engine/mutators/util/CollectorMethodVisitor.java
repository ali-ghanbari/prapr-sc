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

import org.pitest.reloc.asm.Label;
import org.pitest.reloc.asm.MethodVisitor;
import org.pitest.reloc.asm.Opcodes;

import java.util.ArrayList;
import java.util.List;

/**
 * A method visitor used to collect information needed during mutating a method
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
class CollectorMethodVisitor extends MethodVisitor {
    private final Indexer<Label> labelIndexer;

    private final List<RawLocalInfo> locals;

    private final List<LocalVarInfo> localsInfo;

    private static class RawLocalInfo {
        final String name;
        final String desc;
        final Label start;
        final Label end;
        final int index;

        RawLocalInfo(String name, String desc, Label start, Label end, int index) {
            this.name = name;
            this.desc = desc;
            this.start = start;
            this.end = end;
            this.index = index;
        }
    }

    CollectorMethodVisitor(MethodVisitor mv) {
        super(Opcodes.ASM6, mv);
        this.labelIndexer = new Indexer<>();
        this.locals = new ArrayList<>();
        this.localsInfo = new ArrayList<>();
    }

    @Override
    public void visitLabel(Label label) {
        this.labelIndexer.index(label);
        super.visitLabel(label);
    }

    @Override
    public void visitLocalVariable(String name,
                                   String desc,
                                   String signature,
                                   Label start,
                                   Label end,
                                   int index) {
        this.locals.add(new RawLocalInfo(name, desc, start, end, index));
        super.visitLocalVariable(name, desc, signature, start, end, index);
    }

    @Override
    public void visitEnd() {
        for (final RawLocalInfo rli : this.locals) {
            final LocalVarInfo lvi;
            lvi = new LocalVarInfo(rli.name,
                    rli.desc,
                    rli.index,
                    this.labelIndexer.indexOf(rli.start),
                    this.labelIndexer.indexOf(rli.end));
            this.localsInfo.add(lvi);
        }
        super.visitEnd();
    }

    public List<LocalVarInfo> getLocalsInfo() {
        return localsInfo;
    }
}