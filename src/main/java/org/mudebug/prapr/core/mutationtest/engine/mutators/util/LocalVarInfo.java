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

import org.pitest.reloc.asm.Type;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class LocalVarInfo {
    public final String name;

    public final String typeDescriptor;

    public final int index;

    public final int startsAt;

    public final int endsAt;
    
    LocalVarInfo(String name, String typeDescriptor, int index, int startsAt, int endsAt) {
        if (startsAt < 0 || endsAt < 0) {
            throw new IllegalArgumentException("undefined scope");
        }
        this.name = name;
        this.typeDescriptor = typeDescriptor;
        this.index = index;
        this.startsAt = startsAt;
        this.endsAt = endsAt;
    }
    
    public Type getType() {
        return Type.getType(typeDescriptor);
    }

    @Override
    public String toString() {
        return "LocalVarInfo [name=" + name + ", typeDescriptor=" + typeDescriptor + ", index=" + index + ", startsAt="
                + startsAt + ", endsAt=" + endsAt + "]";
    }
}