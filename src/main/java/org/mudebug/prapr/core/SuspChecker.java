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

import org.pitest.mutationtest.engine.MutationDetails;

import java.io.Serializable;
import java.util.Collection;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public interface SuspChecker extends Serializable {
    /**
     * class-level suspiciousness check to avoid mutating classes that are not covered by any failing test
     * @param className the name of the class to be checked
     * @return <code>true</code> iff the class should be mutated
     */
    boolean isHit(String className);

    /**
     * method-level suspiciousness check to avoid mutating methods that are not covered by any failing test
     * NOTE: methodSig = method_name + method_desc
     * @param className the name of the class to be checked
     * @param methodSig the signature of the method to be checked
     * @return <code>true</code> iff the method should be mutated
     */
    boolean isHit(String className, String methodSig);

    /**
     * instruction-level suspiciousness check to avoid mutating instructions that are not covered by any failing test
     * @param details the details of the mutation point
     * @return <code>true</code> iff the instruction should be mutated
     */
    boolean isHit(MutationDetails details);

    @Deprecated
    Collection<String> getAllFailingTests();
}