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
import java.util.Map;

/**
 *
 * @param <T> type of the items stored in the data structure
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class Indexer<T> {
    private final Map<T, Integer> indexMap;

    private int index;
    
    public Indexer() {
        this.indexMap = new HashMap<>();
        this.index = 0;
    }
    
    public int index(T t) {
        final int i = index++;
        indexMap.put(t, i);
        return i;
    }
    
    public int indexOf(T t) {
        Integer i = indexMap.get(t);
        return i == null ? -1 : i.intValue();
    }
}