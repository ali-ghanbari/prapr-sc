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

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class ScopeTracker {
    private final List<LocalVarInfo> localsInfo;
    private final Indexer<Label> labelIndexer;
    public final List<LocalVarInfo> visibleLocals;

    public ScopeTracker(List<LocalVarInfo> localsInfo) {
        this.localsInfo = localsInfo;
        this.labelIndexer = new Indexer<>();
        this.visibleLocals = new ArrayList<>();
    }
    
    private void kill(final int labelIndex) {
        Iterator<LocalVarInfo> lvit = this.visibleLocals.iterator();
        while (lvit.hasNext()) {
            if (lvit.next().endsAt == labelIndex) {
                lvit.remove();
            }
        }
    }
    
    private void gen(final int labelIndex) {
        for (final LocalVarInfo lvi : this.localsInfo) {
            if (lvi.startsAt == labelIndex) {
                this.visibleLocals.add(lvi);
            }
        }
    }
    
    public void transfer(Label label) {
        final int index = this.labelIndexer.index(label);
        kill(index);
        gen(index);
    }
    
    public LocalVarInfo find(int index) {
        for (LocalVarInfo lvi : this.visibleLocals) {
            if (lvi.index == index) {
                return lvi;
            }
        }
        return null;
    }
}