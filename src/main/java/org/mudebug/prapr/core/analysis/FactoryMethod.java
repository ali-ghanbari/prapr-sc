package org.mudebug.prapr.core.analysis;

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
import org.pitest.reloc.asm.Type;

import java.io.Serializable;

/**
 * Represents a static method whose type is a subtype of <code>Object</code>.
 * Such methods are likely factory methods.
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class FactoryMethod implements Serializable {
    private static final long serialVersionUID = 1L;

    private final ClassName declaringClassName;

    private final String name;

    private final String desc;

    private final boolean itf;

    protected FactoryMethod(String className, String name, String desc, boolean itf) {
        this.declaringClassName = ClassName.fromString(className);
        this.name = name;
        this.desc = desc;
        this.itf = itf;
    }

    public String getName() {
        return name;
    }

    public String getDescriptor() {
        return desc;
    }

    public Type getReturnType() {
        return Type.getReturnType(desc);
    }


    public ClassName getDeclaringClassName() {
        return declaringClassName;
    }

    public String toString() {
        return String.format("%s.%s%s", declaringClassName.asJavaName(), name, desc);
    }

    @Override
    public int hashCode() {
        final int prime = 31;
        int result = 1;
        result = prime * result + ((declaringClassName == null) ? 0 : declaringClassName.hashCode());
        result = prime * result + ((desc == null) ? 0 : desc.hashCode());
        result = prime * result + ((name == null) ? 0 : name.hashCode());
        return result;
    }

    @Override
    public boolean equals(Object obj) {
        if (this == obj) {
            return true;
        }
        if (obj == null) {
            return false;
        }
        if (getClass() != obj.getClass()) {
            return false;
        }
        FactoryMethod other = (FactoryMethod) obj;
        if (declaringClassName == null) {
            if (other.declaringClassName != null) {
                return false;
            }
        } else if (!declaringClassName.equals(other.declaringClassName)) {
            return false;
        }
        if (desc == null) {
            if (other.desc != null) {
                return false;
            }
        } else if (!desc.equals(other.desc)) {
            return false;
        }
        if (name == null) {
            if (other.name != null) {
                return false;
            }
        } else if (!name.equals(other.name)) {
            return false;
        }
        return true;
    }

    public boolean isOwnerAnInterface() {
        return itf;
    }
}