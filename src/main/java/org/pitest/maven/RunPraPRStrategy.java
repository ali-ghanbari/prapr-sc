package org.pitest.maven;

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

import org.apache.maven.plugin.MojoExecutionException;
import org.mudebug.prapr.entry.mutationtest.tooling.PraPREntryPoint;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.AnalysisResult;
import org.pitest.mutationtest.tooling.CombinedStatistics;
import org.pitest.mutationtest.tooling.EntryPoint;

import java.io.File;
import java.util.Map;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class RunPraPRStrategy implements GoalStrategy {
    @Override
    public CombinedStatistics execute(final File baseDir,
                                      final ReportOptions options,
                                      final PluginServices plugins,
                                      final Map<String, String> environmentVariables) throws MojoExecutionException {
        final EntryPoint entryPoint = new PraPREntryPoint();
        AnalysisResult result = entryPoint.execute(baseDir, options, plugins, environmentVariables);
        if (result.getError().hasSome()) {
            throw new MojoExecutionException("fail", result.getError().value());
        }
        return result.getStatistics().value();
    }
}
