package org.mudebug.prapr.core.mutationtest.engine.config;

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
import org.pitest.functional.predicate.Predicate;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.mutationtest.engine.gregor.config.DefaultMutationEngineConfiguration;

import java.util.Collection;

/**
 * Mutation engine configuration for PraPR mutation engine
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPRMutationEngineConfig extends DefaultMutationEngineConfiguration {
    private final SuspChecker suspChecker;

    private final GlobalInfo classHierarchy;

    public PraPRMutationEngineConfig(final Predicate<MethodInfo> filter,
                                     final Collection<? extends MethodMutatorFactory> mutators,
                                     final SuspChecker suspChecker,
                                     final GlobalInfo classHierarchy) {
        super(filter, mutators);
        this.suspChecker = suspChecker;
        this.classHierarchy = classHierarchy;
    }

    public SuspChecker getSuspChecker() {
        return this.suspChecker;
    }

    public GlobalInfo getClassHierarchy() {
        return this.classHierarchy;
    }
}
