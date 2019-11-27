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

import org.pitest.classinfo.ClassName;
import org.pitest.reloc.asm.Opcodes;

import java.util.List;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class PraPRMethodInfo {
    public final String name;

    public final boolean isStatic;

    public final boolean isPublic;

    public final boolean isPrivate;

    public final boolean isProtected;

    public final List<LocalVarInfo> localsInfo;

    public final ClassName owningClassName;

    public final List<Integer> nullableParamIndices;

    PraPRMethodInfo(final int access,
                    final String name,
                    final List<LocalVarInfo> localsInfo,
                    final ClassName owningClassName,
                    final List<Integer> nullableParamIndices) {
        this.name = name;
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
        this.isPrivate = (access & Opcodes.ACC_PRIVATE) != 0;
        this.isProtected = (access & Opcodes.ACC_PROTECTED) != 0;
        this.localsInfo = localsInfo;
        this.owningClassName = owningClassName;
        this.nullableParamIndices = nullableParamIndices;
    }
}