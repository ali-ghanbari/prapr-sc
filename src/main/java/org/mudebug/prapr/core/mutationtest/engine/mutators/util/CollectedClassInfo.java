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

import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Representing all the information that is gathered for a class to be mutated.
 * These information include details about field and methods (which in turn
 * includes locals and parameter and return types).
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class CollectedClassInfo {
    /*type descriptor -> list of FieldInfo*/
    /*note: in a class we can have multiple fields with the same type*/
    public final Map<String, List<FieldInfo>> fieldsInfo;

    /*method descriptor -> list of MethodInfo*/
    public final Map<String, List<PraPRMethodInfo>> methodsInfo;

    private boolean itf;

    CollectedClassInfo() {
        fieldsInfo = new HashMap<>();
        methodsInfo = new HashMap<>();
    }
    
    public PraPRMethodInfo findMethod(String name, String desc) {
        final List<PraPRMethodInfo> mil = this.methodsInfo.get(desc);
        for (final PraPRMethodInfo mi : mil) {
            if (mi.name.equals(name)) {
                return mi;
            }
        }
        return null;
    }

    public boolean isInterface() {
        return itf;
    }

    public void setInterface(boolean itf) {
        this.itf = itf;
    }
}