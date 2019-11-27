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
import java.io.FileOutputStream;
import java.io.Writer;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.mudebug.prapr.core.SuspStrategy;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;
import org.pitest.mutationtest.ClassMutationResults;
import org.pitest.mutationtest.DetectionStatus;
import org.pitest.mutationtest.MutationResult;
import org.pitest.mutationtest.MutationResultListener;
import org.pitest.mutationtest.engine.Mutater;
import org.pitest.mutationtest.engine.MutationDetails;
import org.pitest.util.ResultOutputStrategy;

import static org.mudebug.prapr.entry.report.log.Commons.calculateSusp;
import static org.mudebug.prapr.entry.report.log.Commons.sanitizeMutatorName;

/**
 * Dirty but fast!
 *
 * @author Ali Ghanbari (ali.ghanbari@utdallas.edu)
 * @since 1.0.0
 */
public class LOGReportListener implements MutationResultListener {
    private final Map<MutationDetails, Integer> allRanks;

    private final Map<MutationDetails, Integer> plRanks;

    private final Map<MutationDetails, File> dumpFiles;

    private final Writer out;

    private final Map<String, Double> mutatorScore;

    private final List<MutationResult> killedMutations;

    private final List<MutationResult> survivedMutations;

    private final Set<MutationResultWrapper> allMutations;

    private final SuspStrategy suspStrategy;

    private final Collection<String> failingTests;

    private final int allTestsCount;

    private final Mutater mutater;

    private final File poolDirectory;

    private final boolean shouldDumpMutations;
    
    public LOGReportListener(final ResultOutputStrategy outStrategy,
                             final File poolDirectory,
                             final SuspStrategy suspStrategy,
                             final Collection<String> failingTests,
                             final int allTestsCount,
                             final Mutater mutater,
                             final boolean shouldDumpMutations) {
        this.poolDirectory = poolDirectory;
        this.out = outStrategy.createWriterForFile("fix-report.log");
        this.mutatorScore = new HashMap<>();
        this.killedMutations = new ArrayList<>();
        this.survivedMutations = new ArrayList<>();
        this.allMutations = new HashSet<>();
        this.suspStrategy = suspStrategy;
        this.failingTests = failingTests;
        this.allTestsCount = allTestsCount;
        this.allRanks = new HashMap<>();
        this.plRanks = new HashMap<>();
        this.dumpFiles = new HashMap<>();
        this.mutater = mutater;
        this.shouldDumpMutations = shouldDumpMutations;
    }
    
    private void writeln(final String s) {
        try {
            this.out.write(s + "\n");
        } catch (Exception e) {
            e.printStackTrace();
        }
    }
    
    private void computeMutatorScores() {
        final Map<String, Integer> totalCount = new HashMap<>();
        final Map<String, Integer> plausibleCount = new HashMap<>();
        for (final MutationResult mr : this.killedMutations) {
            final String mutator = sanitizeMutatorName(mr.getDetails().getMutator());
            inc(totalCount, mutator);
        }
        for (final MutationResult mr : this.survivedMutations) {
            final String mutator = sanitizeMutatorName(mr.getDetails().getMutator());
            inc(plausibleCount, mutator);
            inc(totalCount, mutator);
        }
        for (Map.Entry<String, Integer> totalEnt : totalCount.entrySet()) {
            final String mutator = totalEnt.getKey();
            Integer num = plausibleCount.get(mutator);
            if (num == null) {
                num = 0;
            }
            Integer denum = totalEnt.getValue();
            Double score = num.doubleValue() / denum.doubleValue();
            mutatorScore.put(mutator, score);
        }
    }
    
    private List<List<MutationDetails>> groupAndSortBySusp(final List<List<MutationResult>> mutationsSuperList) {
        Map<Double, List<MutationDetails>> mutationsGroupedMap = new HashMap<>();
        for (final List<MutationResult> mrl : mutationsSuperList) {
            for (final MutationResult mr : mrl) {
                final MutationDetails md = mr.getDetails();
                final Double susp = calculateSusp(this.suspStrategy, md, this.failingTests, this.allTestsCount);
                List<MutationDetails> group = mutationsGroupedMap.get(susp);
                if (group == null) {
                    group = new ArrayList<>();
                    mutationsGroupedMap.put(susp, group);
                }
                group.add(md);
            }
        }
        List list;
        list = new ArrayList<>(mutationsGroupedMap.entrySet());
        mutationsGroupedMap = null; // free the space
        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                final Map.Entry<Double,?> e1 = (Map.Entry<Double,?>) o1;
                final Map.Entry<Double, ?> e2 = (Map.Entry<Double, ?>) o2;
                return Double.compare(e2.getKey(), e1.getKey());
            }
        });
        for (int i = 0; i < list.size(); i++) {
            list.set(i, ((Map.Entry<?,?>) list.get(i)).getValue());
        }
        return list;
    }
    
    private List<List<MutationDetails>> groupAndSortByMutScore(final List<MutationDetails> chunk) {
        Map<Double, Collection<MutationDetails>> mutationsGroupedMap = FCollection.bucket(chunk,
                new F<MutationDetails, Double>() {
                    @Override
                    public Double apply(MutationDetails md) {
                        return LOGReportListener.this.mutatorScore.get(sanitizeMutatorName(md.getMutator()));
                    }
                });
        List list;
        list = new ArrayList<>(mutationsGroupedMap.entrySet());
        mutationsGroupedMap = null;
        Collections.sort(list, new Comparator() {
            @Override
            public int compare(Object o1, Object o2) {
                final Map.Entry<Double,?> e1 = (Map.Entry<Double,?>) o1;
                final Map.Entry<Double, ?> e2 = (Map.Entry<Double, ?>) o2;
                return Double.compare(e1.getKey(), e2.getKey());
            }
        });
        for (int i = 0; i < list.size(); i++) {
            list.set(i, ((Map.Entry<?,?>) list.get(i)).getValue());
        }
        return list;
    }
    
    private void doRanking(final SetRankFunction setRankFunction,
            final List<List<MutationResult>> mutationsSuperList) {
        final List list = groupAndSortBySusp(mutationsSuperList);
        // type of list is List<List<MutationDetails>>
        for (int i = 0; i < list.size(); i++) {
            list.set(i, groupAndSortByMutScore((List<MutationDetails>) list.get(i)));
        }
        // type of list is List<List<List<MutationDetails>>>
        final List<List<MutationDetails>> finalSorted = (List<List<MutationDetails>>) FCollection.flatten(list);
        int rank = 0;
        for (List<MutationDetails> mdl : finalSorted) {
            rank += mdl.size();
            for (MutationDetails md : mdl) {
                setRankFunction.setRank(md, rank);
            }
        }
    }
    
    private void inc(Map<String, Integer> countMap, String mutator) {
        Integer count = countMap.get(mutator);
        if (count == null) {
            count = 0;
        }
        countMap.put(mutator, count + 1);
    }
    
    private void thickLine() {
        writeln("================================================");
    }
    
    private void thinLine() {
        writeln("------------------------------------------------");
    }

    @Override
    public void runStart() {
        writeln("PraPR 2 (JDK 1.7) Fix Report - " + (new Date()).toString());
    }

    @Override
    public void handleMutationResult(ClassMutationResults results) {
        for (final MutationResult mr : results.getMutations()) {
            this.allMutations.add(MutationResultWrapper.wrap(mr));
        }
    }
    
    private void printRankedList() {
        final int plausiblesSize = this.survivedMutations.size();
        final int totalSize = this.killedMutations.size() + plausiblesSize;
        writeln("Number of Plausible Fixes: " + plausiblesSize);
        writeln("Total Number of Patches: " + totalSize);
        thickLine();
        if (plausiblesSize == 0) {
            writeln("No fix found!");
            return;
        }
        computeMutatorScores();
        doRanking(new SetRankFunction() {
            @Override
            public void setRank(MutationDetails md, int rank) {
                LOGReportListener.this.plRanks.put(md, rank);
            }
        }, Arrays.asList(this.survivedMutations));
        doRanking(new SetRankFunction() {
            @Override
            public void setRank(MutationDetails md, int rank) {
                LOGReportListener.this.allRanks.put(md, rank);
            }
        }, Arrays.asList(this.survivedMutations, this.killedMutations));
        Collections.sort(this.survivedMutations, new Comparator<MutationResult>() {
            @Override
            public int compare(MutationResult mr1, MutationResult mr2) {
                final MutationDetails md1 = mr1.getDetails();
                final MutationDetails md2 = mr2.getDetails();
                final int rank1 = LOGReportListener.this.plRanks.get(md1);
                final int rank2 = LOGReportListener.this.plRanks.get(md2);
                return Integer.compare(rank1, rank2);
            }
        });
        for (int i = 0; i < this.survivedMutations.size(); i++) {
            final MutationDetails md = this.survivedMutations.get(i).getDetails();
            printMutationDetails(1 + i, md);
            thinLine();
        }
    }
    
    private void printMutationDetails(int row, MutationDetails md) {
        writeln(String.format("%d.", row));
        writeln(String.format("\tMutator: %s", sanitizeMutatorName(md.getMutator())));
        writeln(String.format("\tDescription: %s", md.getDescription()));
        final String filePath;
        final String mutatedClass = md.getClassName().asInternalName();
        final int lastSlash = mutatedClass.lastIndexOf('/');
        if (lastSlash >= 0) {
            filePath = mutatedClass.substring(0, 1 + lastSlash);
        } else {
            filePath = "";
        }
        writeln(String.format("\tFile Name: %s%s", filePath, md.getFilename()));
        writeln(String.format("\tLine Number: %d", md.getLineNumber()));
        writeln(String.format("\tRank: %d", this.plRanks.get(md)));
        writeln(String.format("\tTotal Rank: %d", this.allRanks.get(md)));
        final File dumpFile = this.dumpFiles.get(md);
        if (dumpFile != null) {
            writeln(String.format("\tDump: %s", dumpFile.getName()));
            if (this.shouldDumpMutations) {
                try (final FileOutputStream fos = new FileOutputStream(dumpFile)) {
                    final byte[] bytes = this.mutater.getMutation(md.getId()).getBytes();
                    fos.write(bytes);
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }
        }
    }

    private File getDumpFile() {
        final int id = this.survivedMutations.size();
        return new File(this.poolDirectory, "mutant-" + id + ".class");
    }

    private void multiplexMutations() {
        final Iterator<MutationResultWrapper> mit = this.allMutations.iterator();
        while (mit.hasNext()) {
            final MutationResult mr = mit.next().getMutationResult();
            if (mr.getStatus() == DetectionStatus.SURVIVED) {
                    this.survivedMutations.add(mr);
                    if (this.shouldDumpMutations) {
                        this.dumpFiles.put(mr.getDetails(), getDumpFile());
                    }
                } else {
                    this.killedMutations.add(mr);
                }
            mit.remove();
        }
    }

    @Override
    public void runEnd() {
        try {
            multiplexMutations();
            if (this.shouldDumpMutations) {
                this.poolDirectory.mkdirs();
            }
            printRankedList();
            this.out.close();
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

}
