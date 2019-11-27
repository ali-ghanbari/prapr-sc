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

import static org.junit.Assert.*;

import static org.mudebug.prapr.core.commons.TestCaseUtil.sanitizeTestName;

/**
 * @author Ali Ghanbari
 */
public class TestCaseUtilTest {

    @org.junit.Test
    public void testSanitizeTestName() {
        String testInput = "com.example.test(blah.blah)";
        String expected = "com.example.test";
        assertEquals(expected, sanitizeTestName(testInput));
        testInput = "com.example:test(blah.blah)";
        expected = "com.example.test";
        assertEquals(expected, sanitizeTestName(testInput));
        testInput = "com.example::test(blah.blah)";
        expected = "com.example.test";
        assertEquals(expected, sanitizeTestName(testInput));
        testInput = "com.example:test";
        expected = "com.example.test";
        assertEquals(expected, sanitizeTestName(testInput));
        testInput = "com.example::test";
        expected = "com.example.test";
        assertEquals(expected, sanitizeTestName(testInput));
    }
}