package org.mudebug.prapr.entry.report;

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

import org.mudebug.prapr.core.SuspStrategy;
import org.mudebug.prapr.core.commons.TestCaseUtil;
import org.pitest.coverage.TestInfo;
import org.pitest.mutationtest.engine.MutationDetails;

import java.util.Collection;
import java.util.HashSet;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class Commons {
    private Commons() {

    }

    public static String sanitizeMutatorName(String mutator) {
        mutator = mutator.substring(1 + mutator.lastIndexOf('.'));
        if (Character.isDigit(mutator.charAt(mutator.length() - 1))) {
            final int lastIndex = mutator.lastIndexOf('_');
            mutator = mutator.substring(0, lastIndex);
        }
        return mutator;
    }

    public static double calculateSusp(final SuspStrategy suspStrategy,
                                       final MutationDetails mutationDetails,
                                       Collection<String> failingTests,
                                       final int allTestsCount) {
        final int allFailingTestsCount = failingTests.size();
        failingTests = new HashSet<>(failingTests); // make a copy
        int ef = 0;
        int ep = 0;
        for (final TestInfo ti : mutationDetails.getTestsInOrder()) {
            if (TestCaseUtil.contains(failingTests, ti)) {
                ef++;
                TestCaseUtil.remove(failingTests, ti);
            } else {
                ep++;
            }
        }
        final int nf = failingTests.size();
        final int np = allTestsCount - allFailingTestsCount - ep;
        return suspStrategy.computeSusp(ef, ep, nf, np);
    }
}
