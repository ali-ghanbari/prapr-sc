package org.mudebug.prapr.entry.mutationtest.tooling;

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

import org.mudebug.prapr.core.SuspChecker;
import org.mudebug.prapr.core.analysis.GlobalInfo;
import org.mudebug.prapr.core.mutationtest.AugmentedEngineArguments;
import org.mudebug.prapr.entry.coverage.execute.PraPRCoverageGenerator;
import org.mudebug.prapr.entry.mutationtest.AugmentedListenerArguments;
import org.mudebug.prapr.entry.mutationtest.build.PraPRTestPrioritizer;
import org.pitest.classinfo.ClassByteArraySource;
import org.pitest.classinfo.ClassInfo;
import org.pitest.classinfo.ClassName;
import org.pitest.classinfo.HierarchicalClassId;
import org.pitest.classpath.ClassPathByteArraySource;
import org.pitest.classpath.ClassloaderByteArraySource;
import org.pitest.classpath.CodeSource;
import org.pitest.coverage.CoverageDatabase;
import org.pitest.coverage.CoverageGenerator;
import org.pitest.coverage.TestInfo;
import org.pitest.functional.FCollection;
import org.pitest.functional.Option;
import org.pitest.help.Help;
import org.pitest.help.PitHelpError;
import org.pitest.maven.PraPRReportOptions;
import org.pitest.mutationtest.EngineArguments;
import org.pitest.mutationtest.HistoryStore;
import org.pitest.mutationtest.ListenerArguments;
import org.pitest.mutationtest.MutationAnalyser;
import org.pitest.mutationtest.MutationConfig;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.build.MutationAnalysisUnit;
import org.pitest.mutationtest.build.MutationGrouper;
import org.pitest.mutationtest.build.MutationInterceptor;
import org.pitest.mutationtest.build.MutationSource;
import org.pitest.mutationtest.build.MutationTestBuilder;
import org.pitest.mutationtest.build.MutationTestUnit;
import org.pitest.mutationtest.build.PercentAndConstantTimeoutStrategy;
import org.pitest.mutationtest.build.TestPrioritiser;
import org.pitest.mutationtest.build.WorkerFactory;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.config.SettingsFactory;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.mutationtest.engine.MutationEngine;
import org.pitest.mutationtest.execute.MutationAnalysisExecutor;
import org.pitest.mutationtest.incremental.DefaultCodeHistory;
import org.pitest.mutationtest.incremental.HistoryListener;
import org.pitest.mutationtest.incremental.IncrementalAnalyser;
import org.pitest.mutationtest.statistics.MutationStatisticsListener;
import org.pitest.mutationtest.statistics.Score;
import org.pitest.mutationtest.tooling.CombinedStatistics;
import org.pitest.mutationtest.tooling.MutationStrategies;
import org.pitest.mutationtest.tooling.SmartSourceLocator;
import org.pitest.mutationtest.tooling.SpinnerListener;
import org.pitest.util.Log;
import org.pitest.util.StringUtil;
import org.pitest.util.Timings;

import java.io.File;
import java.io.IOException;
import java.io.PrintStream;
import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * CREDIT: Most of te body of this class is copied from PIT's <code>MutationCoverage</code> class
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class MutationCoverage {
    private static final int MB = 1024 * 1024;

    private static final Logger LOG = Log.getLogger();

    private final PraPRReportOptions data;

    private final MutationStrategies strategies;

    private final Timings timings;

    private final CodeSource code;

    private final File baseDir;

    private final SettingsFactory settings;

    private final Collection<String> failingTests;

    private final int allTestsCount;

    public MutationCoverage(final MutationStrategies strategies,
                            final File baseDir,
                            final CodeSource code,
                            final ReportOptions data,
                            final SettingsFactory settings,
                            final Timings timings,
                            final Collection<String> failingTests,
                            final int allTestsCount) {
        this.strategies = strategies;
        this.data = (PraPRReportOptions) data;
        this.settings = settings;
        this.timings = timings;
        this.code = code;
        this.baseDir = baseDir;
        this.failingTests = failingTests;
        this.allTestsCount = allTestsCount;
    }

    public CombinedStatistics runReport() throws IOException {
        Log.setVerbose(this.data.isVerbose());
        final Runtime runtime = Runtime.getRuntime();
        if (!this.data.isVerbose()) {
            LOG.info("Verbose logging is disabled. If you encounter an problem please enable it before reporting an issue.");
        }

        LOG.fine("Running report with " + this.data);
        LOG.fine("System class path is " + System.getProperty("java.class.path"));
        LOG.fine("Maximum available memory is " + (runtime.maxMemory() / MB) + " mb");

        final long t0 = System.currentTimeMillis();

        verifyBuildSuitableForMutationTesting();

        checkExcludedRunners();

        final CoverageGenerator coverageGenerator = coverage();

        final CoverageDatabase coverageData = coverageGenerator.calculateCoverage();

        LOG.fine("Used memory after coverage calculation " + ((runtime.totalMemory() - runtime.freeMemory()) / MB) + " mb");
        LOG.fine("Free Memory after coverage calculation " + (runtime.freeMemory() / MB) + " mb");

        final MutationStatisticsListener stats = new MutationStatisticsListener();

        Log.getLogger().info("Collecting some general info about the program...");
        final List<File> expandedClassPathElements = new ArrayList<>();
        expandClassPathElements(data.getClassPathElements(), expandedClassPathElements);
        final GlobalInfo classHierarchy = GlobalInfo.construct(expandedClassPathElements);
        Log.getLogger().info("DONE");

        final SuspChecker suspChecker = ((PraPRCoverageGenerator) coverageGenerator).getSuspChecker();

        EngineArguments args = AugmentedEngineArguments.arguments()
                .withExcludedMethods(data.getExcludedMethods())
                .withMutators(data.getMutators())
                .withSuspChecker(suspChecker)
                .withClassHierarchy(classHierarchy);
        final MutationEngine engine = this.strategies.factory().createEngine(args);

        ClassByteArraySource cbas = fallbackToClassLoader(new ClassPathByteArraySource(this.data.getClassPath()));

        final List<MutationResultListener> config = createConfig(t0, coverageData, stats, engine, cbas);

        history().initialize();

        this.timings.registerStart(Timings.Stage.BUILD_MUTATION_TESTS);
        final List<MutationAnalysisUnit> tus = buildMutationTests(coverageData, engine, cbas, args, suspChecker);
        this.timings.registerEnd(Timings.Stage.BUILD_MUTATION_TESTS);

        LOG.info("Created  " + tus.size() + " mutation test units");
        checkMutationsFound(tus);

        recordClassPath(coverageData);

        LOG.fine("Used memory before analysis start " + ((runtime.totalMemory() - runtime.freeMemory()) / MB) + " mb");
        LOG.fine("Free Memory before analysis start " + (runtime.freeMemory() / MB) + " mb");

        final MutationAnalysisExecutor mae = new MutationAnalysisExecutor(numberOfThreads(), config);
        this.timings.registerStart(Timings.Stage.RUN_MUTATION_TESTS);
        mae.run(tus);
        this.timings.registerEnd(Timings.Stage.RUN_MUTATION_TESTS);

        LOG.info("Completed in " + timeSpan(t0));

        printStats(stats);

        return new CombinedStatistics(stats.getStatistics(), coverageData.createSummary());
    }

    // recommended JVM requirements: JDK 1.7 or JDK 1.8
    private void expandClassPathElements(final Collection<String> classPathElements, final List<File> out) {
        for (final String cpe : classPathElements) {
            final File cpeFile = new File(cpe);
            if (cpeFile.isDirectory()) {
                final File[] files = cpeFile.listFiles();
                for (final File file : files) {
                    if (file.isDirectory()) {
                        expandClassPathElements(Collections.singletonList(file.getAbsolutePath()), out);
                    } else if (file.getName().endsWith(".class")) {
                        out.add(file);
                    }
                }
            } else if (cpeFile.getName().endsWith(".jar")) {
                out.add(cpeFile);
            }
        }
    }

    private void checkExcludedRunners() {
        Collection<String> excludedRunners = this.data.getExcludedRunners();
        if (!excludedRunners.isEmpty()) {
            // Check whether JUnit4 is available or not
            try {
                Class.forName("org.junit.runner.RunWith");
            } catch (ClassNotFoundException e) {
                // JUnit4 is not available on the classpath
                throw new PitHelpError(Help.NO_JUNIT_EXCLUDE_RUNNERS);
            }
        }
    }

    private int numberOfThreads() {
        return Math.max(1, this.data.getNumberOfThreads());
    }

    private List<MutationResultListener> createConfig(final long t0,
                                                      final CoverageDatabase coverageData,
                                                      final MutationStatisticsListener stats,
                                                      final MutationEngine engine,
                                                      final ClassByteArraySource cbas) {
        final List<MutationResultListener> ls = new ArrayList<>();

        ls.add(stats);

        final ListenerArguments args = new AugmentedListenerArguments(this.strategies.output(),
                coverageData, new SmartSourceLocator(this.data.getSourceDirs()),
                engine, cbas, t0, this.data.getSuspStrategy(), this.failingTests, this.allTestsCount,
                this.data.isVerboseReport());

        final MutationResultListener mutationReportListener = this.strategies.listenerFactory()
                .getListener(this.data.getFreeFormProperties(), args);

        ls.add(mutationReportListener);
        ls.add(new HistoryListener(history()));

        if (!this.data.isVerbose()) {
            ls.add(new SpinnerListener(System.out));
        }
        return ls;
    }

    private void recordClassPath(final CoverageDatabase coverageData) {
        final Set<ClassName> allClassNames = getAllClassesAndTests(coverageData);
        final Collection<HierarchicalClassId> ids =
                FCollection.map(this.code.getClassInfo(allClassNames), ClassInfo.toFullClassId());
        history().recordClassPath(ids, coverageData);
    }

    private Set<ClassName> getAllClassesAndTests(final CoverageDatabase coverageData) {
        final Set<ClassName> names = new HashSet<>();
        for (final ClassName each : this.code.getCodeUnderTestNames()) {
            names.add(each);
            FCollection.mapTo(coverageData.getTestsForClass(each), TestInfo.toDefiningClassName(), names);
        }
        return names;
    }

    private void verifyBuildSuitableForMutationTesting() {
        this.strategies.buildVerifier().verify(this.code);
    }

    private void printStats(final MutationStatisticsListener stats) {
        final PrintStream ps = System.out;
        ps.println(StringUtil.separatorLine('='));
        ps.println("- Timings");
        ps.println(StringUtil.separatorLine('='));
        this.timings.report(ps);

        ps.println(StringUtil.separatorLine('='));
        ps.println("- Statistics");
        ps.println(StringUtil.separatorLine('='));
        stats.getStatistics().report(ps);

        ps.println(StringUtil.separatorLine('='));
        ps.println("- Mutators");
        ps.println(StringUtil.separatorLine('='));
        for (final Score each : stats.getStatistics().getScores()) {
            each.report(ps);
            ps.println(StringUtil.separatorLine());
        }
    }

    private List<MutationAnalysisUnit> buildMutationTests(final CoverageDatabase coverageData,
                                                          final MutationEngine engine,
                                                          final ClassByteArraySource bas,
                                                          final EngineArguments args,
                                                          final SuspChecker suspChecker) {
        final MutationConfig mutationConfig = new MutationConfig(engine, coverage().getLaunchOptions());

        final TestPrioritiser testPrioritiser;
        if (this.data.shouldReorderTestCases()) {
            testPrioritiser = new PraPRTestPrioritizer(coverageData, this.failingTests);
        } else {
            testPrioritiser = this.settings.getTestPrioritiser()
                    .makeTestPrioritiser(this.data.getFreeFormProperties(), this.code, coverageData);
        }

        MutationInterceptor interceptor = this.settings.getInterceptor().createInterceptor(this.data, bas);

        final MutationSource source = new MutationSource(mutationConfig, testPrioritiser, bas, interceptor);

        final MutationAnalyser analyser =
                new IncrementalAnalyser(new DefaultCodeHistory(this.code, history()), coverageData);

        final WorkerFactory wf = new WorkerFactory(this.baseDir, coverage().getConfiguration(), mutationConfig, args,
                new PercentAndConstantTimeoutStrategy(this.data.getTimeoutFactor(), this.data.getTimeoutConstant()),
                this.data.isVerbose(), this.data.getClassPath().getLocalClassPath());

        MutationGrouper grouper = this.settings.getMutationGrouper().makeFactory(this.data.getFreeFormProperties(),
                this.code, this.data.getNumberOfThreads(), this.data.getMutationUnitSize());
        final MutationTestBuilder builder = new MutationTestBuilder(wf, analyser, source, grouper);

        final List<MutationAnalysisUnit> result = builder.createMutationTestUnits(this.code.getCodeUnderTestNames());

        filterOutNotHitMutations(result, suspChecker);

        return result;
    }

    private void filterOutNotHitMutations(final List<MutationAnalysisUnit> result,
                                          final SuspChecker suspChecker) {
        try {
            int allMutations = 0;
            int filteredOutMutations = 0;
            for (final MutationAnalysisUnit mau : result) {
                if (mau instanceof MutationTestUnit) {
                    allMutations += mau.priority();
                    final Field availableMutationsField =
                            MutationTestUnit.class.getDeclaredField("availableMutations");
                    availableMutationsField.setAccessible(true);
                    final Collection availableMutations = (Collection) availableMutationsField.get(mau);
                    final Iterator it = availableMutations.iterator();
                    while (it.hasNext()) {
                        if (!suspChecker.isHit((MutationDetails) it.next())) {
                            filteredOutMutations++;
                            it.remove();
                        }
                    }
                }
            }
            Log.getLogger().info(String.format("*** %d/%d MUTATIONS HAVE BEEN FILTERED OUT.", filteredOutMutations, allMutations));
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void checkMutationsFound(final List<MutationAnalysisUnit> tus) {
        if (tus.isEmpty()) {
            if (this.data.shouldFailWhenNoMutations()) {
                throw new PitHelpError(Help.NO_MUTATIONS_FOUND);
            } else {
                LOG.warning(Help.NO_MUTATIONS_FOUND.toString());
            }
        }
    }

    private String timeSpan(final long t0) {
        return "" + ((System.currentTimeMillis() - t0) / 1000) + " seconds";
    }

    private CoverageGenerator coverage() {
        return this.strategies.coverage();
    }

    private HistoryStore history() {
        return this.strategies.history();
    }

    // For reasons not yet understood classes from rt.jar are not resolved for some
    // projects during static analysis phase. For now fall back to the classloader when
    // a class not provided by project classpath
    private ClassByteArraySource fallbackToClassLoader(final ClassByteArraySource bas) {
        final ClassByteArraySource clSource = ClassloaderByteArraySource.fromContext();
        return new ClassByteArraySource() {
            @Override
            public Option<byte[]> getBytes(String clazz) {
                Option<byte[]> maybeBytes = bas.getBytes(clazz);
                if (maybeBytes.hasSome()) {
                    return maybeBytes;
                }
                LOG.log(Level.FINE, "Could not find " + clazz + " on classpath for analysis. Falling back to classloader");
                return clSource.getBytes(clazz);
            }
        };
    }


}