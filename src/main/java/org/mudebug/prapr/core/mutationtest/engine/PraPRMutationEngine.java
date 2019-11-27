package org.mudebug.prapr.core.mutationtest.engine;

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

import org.mudebug.prapr.core.SuspChecker;
import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.engine.config.PraPRMutationEngineConfig;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.functional.predicate.Predicate;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;

import java.util.Collection;
import java.util.LinkedHashSet;
import java.util.Set;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPRMutationEngine implements MutationEngine {
    private final Set<MethodMutatorFactory> mutators;

    private final Predicate<MethodInfo> methodFilter;

    private final SuspChecker suspChecker;

    private final GlobalInfo classHierarchy;

    public PraPRMutationEngine(PraPRMutationEngineConfig config) {
        this.methodFilter = config.methodFilter();
        this.mutators = new LinkedHashSet<>(config.mutators());
        this.suspChecker = config.getSuspChecker();
        this.classHierarchy = config.getClassHierarchy();
    }

    @Override
    public Mutater createMutator(ClassByteArraySource source) {
        return new PraPRMutater(this.methodFilter, this.mutators, source, this.suspChecker, this.classHierarchy);
    }

    @Override
    public Collection<String> getMutatorNames() {
        return FCollection.map(this.mutators, toName());
    }

    @Override
    public String getName() {
        return "prapr";
    }

    private static F<MethodMutatorFactory, String> toName() {
        return new F<MethodMutatorFactory, String>() {
            @Override
            public String apply(final MethodMutatorFactory a) {
                return a.getName();
            }
        };
    }

    @Override
    public String toString() {
        return "PraPRMutationEngine{" +
                "mutationOperators=" + mutators +
                ", methodFilter=" + methodFilter +
                ", suspChecker=" + suspChecker +
                ", classHierarchy=" + classHierarchy +
                '}';
    }
}
