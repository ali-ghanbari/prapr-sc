package org.mudebug.prapr.entry.mutationtest.build;

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

import org.mudebug.prapr.core.commons.TestCaseUtil;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.build.DefaultTestPrioritiser;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.Collection;
import java.util.List;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPRTestPrioritizer extends DefaultTestPrioritiser {
    private final Collection<String> failingTests;

    public PraPRTestPrioritizer(CoverageDatabase coverage, final Collection<String> failingTests) {
        super(coverage);
        this.failingTests = failingTests;
    }

    @Override
    public List<TestInfo> assignTests(MutationDetails mutation) {
        final List<TestInfo> sortedTestCases = super.assignTests(mutation);
        return TestCaseUtil.reorder(sortedTestCases, this.failingTests);
    }
}
