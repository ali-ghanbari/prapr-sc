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

/**
 * A class containing the information for a field.
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class FieldInfo {
    public final boolean isStatic;

    public final boolean isPublic;

    public final boolean isFinal;

    public final String name;

    public final ClassName owningClassName;

    FieldInfo(int access, String name, ClassName owningClassName) {
        this.isStatic = (access & Opcodes.ACC_STATIC) != 0;
        this.isPublic = (access & Opcodes.ACC_PUBLIC) != 0;
        this.isFinal = (access & Opcodes.ACC_FINAL) != 0;
        this.name = name;
        this.owningClassName = owningClassName;
    }
}