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

import org.mudebug.prapr.core.SuspCheckerType;
import org.mudebug.prapr.core.SuspStrategy;
import org.pitest.mutationtest.config.ReportOptions;

import java.io.File;
import java.util.ArrayList;
import java.util.Collection;

/**
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 2.0.3
 */
public class PraPRReportOptions extends ReportOptions {
    private SuspCheckerType mutateSuspStmt;

    private Collection<String> failingTests;

    private boolean reorderTestCases;

    private SuspStrategy suspStrategy;

    private boolean verboseReport;

    public PraPRReportOptions() {
        this.failingTests = new ArrayList<>();
    }

    public PraPRReportOptions(final ReportOptions reportOptions) {
        this.failingTests = new ArrayList<>();
        setTargetClasses(reportOptions.getTargetClasses());
        setExcludedMethods(reportOptions.getExcludedMethods());
        setExcludedClasses(reportOptions.getExcludedClasses());
        setExcludedTestClasses(reportOptions.getExcludedTestClasses());
        setCodePaths(reportOptions.getCodePaths());
        setReportDir(makePraPRReportDirectory(reportOptions.getReportDir()));
        setHistoryInputLocation(reportOptions.getHistoryInputLocation());
        setHistoryOutputLocation(reportOptions.getHistoryOutputLocation());
        setSourceDirs(reportOptions.getSourceDirs());
        setMutators(reportOptions.getMutators());
        setFeatures(reportOptions.getFeatures());
        setDependencyAnalysisMaxDistance(reportOptions.getDependencyAnalysisMaxDistance());
        addChildJVMArgs(reportOptions.getJvmArgs());
        setNumberOfThreads(reportOptions.getNumberOfThreads());
        setTimeoutFactor(reportOptions.getTimeoutFactor());
        setTimeoutConstant(reportOptions.getTimeoutConstant());
        setTargetTests(reportOptions.getTargetTests());
        setLoggingClasses(reportOptions.getLoggingClasses());
        setVerbose(reportOptions.isVerbose());
        setFailWhenNoMutations(reportOptions.shouldFailWhenNoMutations());
        addOutputFormats(reportOptions.getOutputFormats());
        setGroupConfig(reportOptions.getGroupConfig());
        setMutationUnitSize(reportOptions.getMutationUnitSize());
        setShouldCreateTimestampedReports(reportOptions.shouldCreateTimeStampedReports());
        setDetectInlinedCode(reportOptions.isDetectInlinedCode());
        setExportLineCoverage(reportOptions.shouldExportLineCoverage());
        setMutationThreshold(reportOptions.getMutationThreshold());
        setCoverageThreshold(reportOptions.getCoverageThreshold());
        setMutationEngine(reportOptions.getMutationEngine());
        setJavaExecutable(reportOptions.getJavaExecutable());
        setIncludeLaunchClasspath(reportOptions.isIncludeLaunchClasspath());
        setFreeFormProperties(reportOptions.getFreeFormProperties());
        setMaximumAllowedSurvivors(reportOptions.getMaximumAllowedSurvivors());
        setExcludedRunners(reportOptions.getExcludedRunners());
        setIncludedTestMethods(reportOptions.getIncludedTestMethods());
        setTestPlugin(reportOptions.getTestPlugin());
        setClassPathElements(reportOptions.getClassPathElements());
    }

    private String makePraPRReportDirectory(final String pitReportDirectory) {
        final File pit = new File(pitReportDirectory);
        return (new File(pit.getParentFile(), "prapr-reports")).getAbsolutePath();
    }

    public SuspCheckerType getMutateSuspStmt() {
        return mutateSuspStmt;
    }

    public void setMutateSuspStmt(SuspCheckerType mutateSuspStmt) {
        this.mutateSuspStmt = mutateSuspStmt;
    }

    public Collection<String> getFailingTests() {
        return failingTests;
    }

    public void addFailingTests(Collection<String> failingTests) {
        this.failingTests.addAll(failingTests);
    }

    public boolean shouldReorderTestCases() {
        return reorderTestCases;
    }

    public void setReorderTestCases(boolean reorderTestCases) {
        this.reorderTestCases = reorderTestCases;
    }

    public SuspStrategy getSuspStrategy() {
        return suspStrategy;
    }

    public void setSuspStrategy(SuspStrategy suspStrategy) {
        this.suspStrategy = suspStrategy;
    }

    public boolean isVerboseReport() {
        return verboseReport;
    }

    public void setVerboseReport(boolean verboseReport) {
        this.verboseReport = verboseReport;
    }
}
