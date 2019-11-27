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
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

import org.mudebug.prapr.core.commons.TestCaseUtil;
import org.pitest.coverage.BlockLocation;
import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.engine.Location;
import org.pitest.mutationtest.engine.MutationDetails;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class WeakSuspChecker implements SuspChecker {
    private static final long serialVersionUID = 1L;

    private final Collection<String> failingTests;

    private final Map<String, Collection<String>> coverage;
    
    /**
     * 
     * @param failingTests The collection of originally failing tests (the names are sanitized)
     * @param rawCoverageInfo A mapping that indicates the set of test cases covering each location  
     */
    public WeakSuspChecker(final Collection<String> failingTests,
                           final Collection<Map.Entry<BlockLocation, Set<TestInfo>>> rawCoverageInfo) {
        this.failingTests = new HashSet<>(failingTests);
        this.coverage = new HashMap<>();
        for (final Map.Entry<BlockLocation, Set<TestInfo>> entry : rawCoverageInfo) {
            if (TestCaseUtil.intersect(entry.getValue(), failingTests)) {
                final Location loc = entry.getKey().getLocation();
                final String className = loc.getClassName().asJavaName();
                Collection<String> methods = this.coverage.get(className);
                if (methods == null) {
                    methods = new HashSet<>();
                    this.coverage.put(className, methods);
                }
                methods.add(loc.getMethodName().name() + loc.getMethodDesc());
            }
        }
    }

    @Override
    public boolean isHit(String className) {
        return this.coverage.containsKey(className);
    }

    @Override
    public boolean isHit(String className, String methodSig) {
        final Collection<String> methods = this.coverage.get(className);
        if (methods != null) {
            return methods.contains(methodSig);
        }
        return false;
    }

    @Override
    public boolean isHit(MutationDetails details) {
        return TestCaseUtil.intersect(details.getTestsInOrder(), this.failingTests);
    }

    @Override
    public Collection<String> getAllFailingTests() {
       return this.failingTests;
    }

}
