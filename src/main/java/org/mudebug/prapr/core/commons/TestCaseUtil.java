package org.mudebug.prapr.core.commons;

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

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.pitest.coverage.TestInfo;

/**
 * A set of utility methods for manipulating test case and testing names.
 *
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public final class TestCaseUtil {
    public static String sanitizeTestName(String name) {
        //SETLab style: test.class.name:test_name
        name = name.replace(':', '.');
        //Defects4J style: test.class.name::test_name
        name = name.replace("..", ".");
        int indexOfLP = name.indexOf('(');
        if (indexOfLP >= 0) {
            name = name.substring(0, indexOfLP);
        }
        return name;
    }
    
    public static boolean equals(TestInfo ti, String sanitizedName) {
        return sanitizeTestName(ti.getName()).equals(sanitizedName);
    }
    
    public static boolean contains(Collection<TestInfo> tis, String sanitizedName) {
        for (final TestInfo ti : tis) {
            if (equals(ti, sanitizedName)) {
                return true;
            }
        }
        return false;
    }
    
    public static boolean containsAll(Collection<TestInfo> tis, Collection<String> sanitizedNames) {
        for (final String name : sanitizedNames) {
            if (!contains(tis, name)) {
                return false;
            }
        }
        return true;
    }
    
    public static boolean intersect(Collection<TestInfo> tis, Collection<String> sanitizedNames) {
        for (final String name : sanitizedNames) {
            if (contains(tis, name)) {
                return true;
            }
        }
        return false;
    }
    
    public static String remove(Collection<String> sanitizedNames, final TestInfo ti) {
        final Iterator<String> snit = sanitizedNames.iterator();
        while (snit.hasNext()) {
            final String sanitizedName = snit.next();
            if (equals(ti, sanitizedName)) {
                snit.remove();
                return sanitizedName;
            }
        }
        return null;
    }
    
    public static boolean contains(Collection<String> failingTestSanitizedNames, TestInfo ti) {
        final String tiName = sanitizeTestName(ti.getName());
        for (final String sanitizedName : failingTestSanitizedNames) {
            if (tiName.equals(sanitizedName)) {
                return true;
            }
        }
        return false;
    }
    
    public static List<TestInfo> reorder(final List<TestInfo> tis,
                                         final Collection<String> failingTestSanitizedNames) {
        final List<TestInfo> result = new ArrayList<>(tis.size());
        final List<TestInfo> passingTests = new ArrayList<>(tis.size());
        for (final TestInfo ti : tis) {
            if (contains(failingTestSanitizedNames, ti)) {
                result.add(ti); // failing tests appear first
            } else {
                passingTests.add(ti); 
            }
        }
        // passing tests come next
        result.addAll(passingTests);
        return result;
    }
    
    public static List<TestInfo> union(final Iterator<List<TestInfo>> lit) {
        final List<TestInfo> l = lit.next();
        if (!lit.hasNext()) {
            return l;
        }
        final Set<TestInfo> result = new HashSet<>(l);
        while (lit.hasNext()) {
            result.addAll(lit.next());
        }
        return new ArrayList<>(result);
    }
}