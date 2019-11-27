package org.mudebug.prapr.entry.report.log;

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

import java.io.File;
import java.lang.reflect.Field;
import java.util.Properties;

import org.mudebug.prapr.entry.mutationtest.AugmentedListenerArguments;
import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.MutationResultListenerFactory;
import org.pitest.mutationtest.config.DirectoryResultOutputStrategy;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.util.ResultOutputStrategy;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class LOGReportFactory implements MutationResultListenerFactory {

    @Override
    public String description() {
        return "PraPR log report plugin";
    }

    @Override
    public MutationResultListener getListener(final Properties props,
                                              final ListenerArguments args) {
        final AugmentedListenerArguments arguments = (AugmentedListenerArguments) args;
        final Mutater mutater = arguments.getEngine().createMutator(arguments.getClassByteArraySource());
        final File poolDirectory = new File(getReportDirectory(arguments.getOutputStrategy()), "pool");
        return new LOGReportListener(arguments.getOutputStrategy(), poolDirectory, arguments.getSuspStrategy(),
                arguments.getFailingTests(), arguments.getAllTestsCount(), mutater, arguments.shouldDumpMutations());
    }

    private File getReportDirectory(final ResultOutputStrategy outputStrategy) {
        try {
            if (outputStrategy instanceof DirectoryResultOutputStrategy) {
                final Field reportDirField = outputStrategy.getClass().getDeclaredField("reportDir");
                reportDirField.setAccessible(true);
                return (File) reportDirField.get(outputStrategy);
            }
        } catch (Exception e) {
        }
        throw new IllegalArgumentException();
    }

    @Override
    public String name() {
        return "LOG";
    }

}