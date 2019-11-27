package org.mudebug.prapr.core;

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

import java.util.Collection;

import org.pitest.mutationtest.engine.MutationDetails;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class DummySuspChecker implements SuspChecker {
    private static final long serialVersionUID = 1L;

    private final Collection<String> failingTests;
    
    public DummySuspChecker(final Collection<String> failingTests) {
        this.failingTests = failingTests;
    }

    @Override
    public boolean isHit(String className) {
        return true;
    }

    @Override
    public boolean isHit(String className, String methodSig) {
        return true;
    }

    @Override
    public boolean isHit(MutationDetails details) {
        return true;
    }

    @Override
    public Collection<String> getAllFailingTests() {
        return this.failingTests;
    }

}