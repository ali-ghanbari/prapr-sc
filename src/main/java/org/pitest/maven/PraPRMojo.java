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
import org.apache.maven.plugin.logging.Log;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.mudebug.prapr.core.SuspCheckerType;
import org.mudebug.prapr.core.SuspStrategyImpl;
import org.mudebug.prapr.core.commons.TestCaseUtil;
import org.mudebug.prapr.core.commons.TextStyleUtil;
import org.pitest.functional.Option;
import org.pitest.mutationtest.config.PluginServices;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.CombinedStatistics;

import java.util.ArrayList;
import java.util.List;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
@Mojo(name = "prapr",
        defaultPhase = LifecyclePhase.VERIFY,
        requiresDependencyResolution = ResolutionScope.TEST,
        threadSafe = true)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class PraPRMojo extends AbstractPitMojo {
    /**
     * If DEFAULT, PraPR mutates only those statements that are covered by all failing test cases.
     */
    @Parameter(property = "mutateSuspStmt", defaultValue = "DEFAULT")
    private String mutateSuspStmt;

    /**
     * The set of originally failing tests in case the user wishes to enumerate them manually.
     * PraPR shall infer failing test cases in case the user leaves this list blank
     */
    @Parameter(property = "failingTests")
    private ArrayList<String> failingTests;

    /**
     * If true, PraPR reorders test cases such that failing test cases are exercised first.
     * By default, this is true.
     */
    @Parameter(property = "reorderTestCases", defaultValue = "true")
    private boolean reorderTestCases;

    /**
     * Determines the formula for computing suspiciousness of mutants.
     * By default, Ochiai suspiciousness value is used.
     */
    @Parameter(property = "suspStrategy", defaultValue = "OCHIAI")
    private String suspStrategy;

    /**
     * If true, PraPR dumps the class files corresponding to the plausible patches so that the users
     * can disassemble the mutated code and get more information about the patch to be applied.
     * NOTE: This might have adverse effects on the performance of the tool.
     * By default, this is true.
     */
    @Parameter(property = "verboseReport", defaultValue = "true")
    private boolean verboseReport;

    /**
     * This constructor is called by Maven
     */
    public PraPRMojo() {
        super(new RunPraPRStrategy(),
                new DependencyFilter(new PluginServices(PraPRMojo.class.getClassLoader())),
                new PluginServices(PraPRMojo.class.getClassLoader()),
                new NonEmptyProjectCheck());
    }

    @Override
    protected Option<CombinedStatistics> analyse() throws MojoExecutionException {
        final PraPRReportOptions data = preanalyse();
        return Option.some(this.goalStrategy.execute(detectBaseDir(), data, this.plugins, getEnvironmentVariables()));
    }

    protected PraPRReportOptions preanalyse() {
        final Log log = getLog();
        log.info(">>>>>>");
        log.info(TextStyleUtil.underlined(TextStyleUtil.bold("PraPR")) + TextStyleUtil.underlined(" (Practical Program Repair via Bytecode Mutation)"));
        log.info("(C) 2019 " + TextStyleUtil.bold("Ali Ghanbari") + ", Samuel Benton, and Lingming Zhang");
        log.info("<<<<<<");

        if (this.failingTests.isEmpty()) {
            log.info("No failing tests specified. PraPR is going to infer failing test cases.");
        } else {
            sanitizeTestCaseNames();
        }

        final List<String> outputFormats = this.getOutputFormats();

        if (outputFormats.isEmpty()) {
            log.info("No output format is specified. PraPR is going to produce LOG and COMPRESSED-XML reports.");
            outputFormats.add("LOG");
            outputFormats.add("COMPRESSED-XML");
        }

        final List<String> activatedMutators = this.getMutators();

        if (activatedMutators.isEmpty()) {
            log.info("No mutator is activated. PraPR is going to activate ALL the mutators.");
            activatedMutators.add("ALL");
        }

        if (this.verboseReport) {
            log.info("PraPR verbose report is activated.");
        }
        final ReportOptions pitReportOptions;
        pitReportOptions = new MojoToReportOptionsConverter(this, new SurefireConfigConverter(), this.filter)
                .convert();
        final PraPRReportOptions data = new PraPRReportOptions(pitReportOptions);
        data.setMutationEngine("prapr");
        data.setMutateSuspStmt(SuspCheckerType.valueOf(this.mutateSuspStmt));
        data.setSuspStrategy(SuspStrategyImpl.valueOf(this.suspStrategy));
        data.addFailingTests(this.failingTests);
        data.setReorderTestCases(this.reorderTestCases);
        data.setVerboseReport(this.verboseReport);
        data.setFailWhenNoMutations(false);
        return data;
    }

    private void sanitizeTestCaseNames() {
        for (int i = 0; i < this.failingTests.size(); i++) {
            this.failingTests.set(i, TestCaseUtil.sanitizeTestName(this.failingTests.get(i)));
        }
    }
}
