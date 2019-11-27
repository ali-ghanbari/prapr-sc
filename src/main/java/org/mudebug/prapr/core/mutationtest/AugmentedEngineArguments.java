package org.mudebug.prapr.core.mutationtest;

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
import org.pitest.mutationtest.EngineArguments;

import java.util.Collection;
import java.util.Collections;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class AugmentedEngineArguments extends EngineArguments {
    private static final long serialVersionUID = 1L;

    private final SuspChecker suspChecker;

    private final GlobalInfo classHierarchy;

    public AugmentedEngineArguments(final Collection<String> mutators,
                                    final Collection<String> excludedMethods,
                                    final SuspChecker suspChecker,
                                    final GlobalInfo classHierarchy) {
        super(mutators, excludedMethods);
        this.suspChecker = suspChecker;
        this.classHierarchy = classHierarchy;
    }

    public static AugmentedEngineArguments arguments() {
        return new AugmentedEngineArguments(Collections.<String>emptyList(),
                Collections.<String>emptyList(),
                null,
                null);
    }

    @Override
    public AugmentedEngineArguments withMutators(Collection<String> mutators) {
        return new AugmentedEngineArguments(mutators, excludedMethods(), this.suspChecker, this.classHierarchy);
    }

    @Override
    public AugmentedEngineArguments withExcludedMethods(Collection<String> excludedMethods) {
        return new AugmentedEngineArguments(mutators(), excludedMethods, this.suspChecker, this.classHierarchy);
    }

    public AugmentedEngineArguments withSuspChecker(SuspChecker suspChecker) {
        return new AugmentedEngineArguments(mutators(), excludedMethods(), suspChecker, this.classHierarchy);
    }

    public AugmentedEngineArguments withClassHierarchy(GlobalInfo classHierarchy) {
        return new AugmentedEngineArguments(mutators(), excludedMethods(), this.suspChecker, classHierarchy);
    }

    public SuspChecker getSuspChecker() {
        return this.suspChecker;
    }

    public GlobalInfo getClassHierarchy() {
        return this.classHierarchy;
    }
}
