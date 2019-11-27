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
import org.mudebug.prapr.core.mutationtest.AugmentedEngineArguments;
import org.mudebug.prapr.core.mutationtest.engine.config.AugmentedMutator;
import org.mudebug.prapr.core.mutationtest.engine.config.PraPRMutationEngineConfig;
import org.pitest.functional.F;
import org.pitest.functional.predicate.Predicate;
import org.pitest.functional.prelude.Prelude;
import org.pitest.mutationtest.EngineArguments;
import org.pitest.mutationtest.MutationEngineFactory;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.engine.gregor.MethodInfo;
import org.pitest.mutationtest.engine.gregor.MethodMutatorFactory;
import org.pitest.util.Glob;

import java.util.Collection;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPREngineFactory implements MutationEngineFactory {
    @Override
    public MutationEngine createEngine(EngineArguments arguments) {
        return createEngine((AugmentedEngineArguments) arguments);
    }

    private MutationEngine createEngine(AugmentedEngineArguments arguments) {
        return createEngineWithMutators(arguments.excludedMethods(),
                createMutatorListFromArrayOrUseDefaults(arguments.mutators()),
                arguments.getSuspChecker(),
                arguments.getClassHierarchy());
    }

    private MutationEngine createEngineWithMutators(final Collection<String> excludedMethods,
                                                    final Collection<? extends MethodMutatorFactory> mutators,
                                                    final SuspChecker suspChecker,
                                                    final GlobalInfo classHierarchy) {
        final Predicate<MethodInfo> filter = Prelude.not(stringToMethodInfoPredicate(excludedMethods));
        final PraPRMutationEngineConfig config =
                new PraPRMutationEngineConfig(filter, mutators, suspChecker, classHierarchy);
        return new PraPRMutationEngine(config);
    }

    @Override
    public String name() {
        return "prapr";
    }

    @Override
    public String description() {
        return "PraPR mutation engine factory";
    }

    private static F<MethodInfo, Boolean> stringToMethodInfoPredicate(final Collection<String> excludedMethods) {
        final Predicate<String> excluded = Prelude.or(Glob.toGlobPredicates(excludedMethods));
        return new Predicate<MethodInfo>() {
            @Override
            public Boolean apply(final MethodInfo methodInfo) {
                return excluded.apply(methodInfo.getName());
            }
        };
    }

    private static Collection<? extends MethodMutatorFactory>
    createMutatorListFromArrayOrUseDefaults(final Collection<String> mutators) {
        if ((mutators != null) && !mutators.isEmpty()) {
            return AugmentedMutator.fromStrings(mutators);
        }
        return AugmentedMutator.all();
    }
}
