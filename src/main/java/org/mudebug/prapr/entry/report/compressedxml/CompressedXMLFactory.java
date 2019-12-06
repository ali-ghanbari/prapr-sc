package org.mudebug.prapr.entry.report.compressedxml;

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

import org.mudebug.prapr.entry.mutationtest.AugmentedListenerArguments;
import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.MutationResultListenerFactory;

import java.util.Properties;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class CompressedXMLFactory implements MutationResultListenerFactory {
    @Override
    public MutationResultListener getListener(Properties props, ListenerArguments args) {
        final AugmentedListenerArguments arguments = (AugmentedListenerArguments) args;
        final CompressedDirectoryResultOutputStrategy cdros =
                CompressedDirectoryResultOutputStrategy.forResultOutputStrategy(arguments.getOutputStrategy());
        return new CompressedXMLReportListener(cdros, arguments.getSuspStrategy(),
                arguments.getFailingTests(), arguments.getAllTestsCount());
    }

    @Override
    public String name() {
        return "COMPRESSED-XML";
    }

    @Override
    public String description() {
        return "Compressed xml report plugin";
    }
}
