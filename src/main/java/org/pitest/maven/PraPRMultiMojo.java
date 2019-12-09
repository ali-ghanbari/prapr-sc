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

import java.io.File;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.plugin.MojoExecutionException;
import org.apache.maven.plugins.annotations.Execute;
import org.apache.maven.plugins.annotations.LifecyclePhase;
import org.apache.maven.plugins.annotations.Mojo;
import org.apache.maven.plugins.annotations.Parameter;
import org.apache.maven.plugins.annotations.ResolutionScope;
import org.apache.maven.project.MavenProject;
import org.pitest.functional.Option;
import org.pitest.mutationtest.config.ReportOptions;
import org.pitest.mutationtest.tooling.CombinedStatistics;

/**
 * Goal to run PraPR on multi-module projects as an entirety
 *
 * @author Lingming Zhang (lingming.zhang@utdallas.edu)
 * @since 2.0.1
 */
@Mojo(name = "praprM", defaultPhase = LifecyclePhase.VERIFY, requiresDependencyResolution = ResolutionScope.TEST, threadSafe = true)
@Execute(phase = LifecyclePhase.TEST_COMPILE)
public class PraPRMultiMojo extends PraPRMojo
{
	/** The list of target modules that should be repaired */
	@Parameter(property = "targetModules")
	protected ArrayList<String> targetModules;

	/** The list of modules that should be ignored from the project */
	@Parameter(property = "excludedModules")
	protected ArrayList<String> excludedModules;

	/**
	 * Override the should run to update the necessary information, including
	 * target classes and tests
	 *
	 * @return a RunDecision to record reasons for skipping the module
	 */
	@Override
	protected RunDecision shouldRun() {
		String projectName = getProject().getArtifactId();
		ModuleUtils.initialize(this);

		updateTargetClasses();
		updateTargetTests();

		RunDecision decision = super.shouldRun();

		if (!isInTargetModules(projectName)) {
			String message = projectName + " is not a target module";
			decision.addReason(message);
		}

		if (isInExcludedModules(projectName)) {
			String message = projectName + " is an excluded module";
			decision.addReason(message);
		}
		return (decision);
	}

	/**
	 * Override the analyse() function to update the PraPR configuration
	 *
	 * @return the results via CombinedStatistics
	 */
	@Override
	protected Option<CombinedStatistics> analyse()
			throws MojoExecutionException {
		// Get normal PraPR configs
		final ReportOptions data=preanalyse();
		// Modify the PraPR config before executing
		modifyPraPRReportOptions(data);
		// Actual entry point is right here for executing PraPR
		return Option.some(this.goalStrategy.execute(detectBaseDir(), data,
				this.plugins, getEnvironmentVariables()));
	}

	/**
	 * Modify the PraPR configuration
	 *
	 * @param data
	 *            The config object
	 */
	public void modifyPraPRReportOptions(ReportOptions data) {

		// reset source directories
		List<String> rawSourceDirs = getTransitiveSourceDirs();
		if (rawSourceDirs != null && !rawSourceDirs.isEmpty()) {
			List<File> sourceDirs = ModuleUtils.namesToFiles(rawSourceDirs);
			data.setSourceDirs(sourceDirs);
		}

		// reset codepaths
		Set<String> codePaths = getTransitiveCodePaths();
		if (data.getCodePaths() != null) {
			Set<String> origCodePaths = new HashSet<String>(
					data.getCodePaths());
			codePaths.addAll(origCodePaths);
		}
		data.setCodePaths(codePaths);

		// reset classpathelements
		List<String> classPathElements = getTransitiveClassPathElements();
		if (data.getClassPathElements() != null) {
			List<String> origClassPathElements = new ArrayList<String>(
					data.getClassPathElements());
			ModuleUtils.addToList(classPathElements, origClassPathElements);
		}
		data.setClassPathElements(classPathElements);
	}

	/**
	 * Get transitive source and test dirs for the current project and all its
	 * dependencies
	 *
	 * @return The list of dirs
	 */
	public List<String> getTransitiveSourceDirs() {
		List<String> transitiveDirs = new ArrayList<String>();
		Set<Artifact> dependencies = getProject().getArtifacts();
		addProjectSourceDirs(transitiveDirs, getProject());

		for (Artifact dependency : dependencies) {
			MavenProject curProject = ModuleUtils
					.getMavenProjectFromName(dependency.getArtifactId());
			if (curProject != null) {
				addProjectSourceDirs(transitiveDirs, curProject);
			}
		}
		return transitiveDirs;
	}

	/**
	 * Add the source dirs for one project
	 *
	 * @param allDirs
	 *            The resulting list
	 * @param project
	 *            The project under analysis
	 */
	public void addProjectSourceDirs(List<String> allDirs,
									 MavenProject project) {
		allDirs.addAll(project.getCompileSourceRoots());
		allDirs.addAll(project.getTestCompileSourceRoots());
	}

	/**
	 * Get the transitivecodepaths from dependencies
	 *
	 * @return The resulting codepaths
	 */
	public Set<String> getTransitiveCodePaths() {
		Set<String> codePathSet = new HashSet<String>();
		Set<Artifact> dependencies = getProject().getArtifacts();

		for (Artifact dependency : dependencies) {
			MavenProject project = ModuleUtils
					.getMavenProjectFromName(dependency.getArtifactId());
			if (project != null) {
				codePathSet.add(project.getBuild().getOutputDirectory());
			}
		}

		return codePathSet;
	}

	/**
	 * Get the transitive classpathelements for all dependencies
	 *
	 * @return The resulting array
	 */
	public List<String> getTransitiveClassPathElements() {
		List<String> classPathElements = new ArrayList<String>();
		Set<Artifact> dependencies = getProject().getArtifacts();

		for (Artifact dependency : dependencies) {
			MavenProject project = ModuleUtils
					.getMavenProjectFromName(dependency.getArtifactId());
			if (project != null) {
				classPathElements.add(project.getBuild().getOutputDirectory());
				classPathElements
						.add(project.getBuild().getTestOutputDirectory());
			}

			if (!dependency.getType().equals("pom")) {
				classPathElements.add(dependency.getFile().getAbsolutePath());
			}
		}

		return classPathElements;
	}

	/**
	 * Update the target classes via analyzing all dependent modules
	 */
	public void updateTargetClasses() {
		List<String> originalTargetClasses = getTargetClasses();
		ArrayList<String> targetClasses = new ArrayList<>();

		if (originalTargetClasses != null && originalTargetClasses.size() > 0) {
			//targetClasses.addAll(originalTargetClasses);
			return;
		} else {
			targetClasses.addAll(ModuleUtils.getSrcClasses(getProject()));
			// handle dependent modules
			List<MavenProject> moduleList = ModuleUtils
					.getDependingModules(getProject());
			for (int i = 0; i < moduleList.size(); i++) {
				MavenProject module = moduleList.get(i);
				List<String> classList = ModuleUtils.getSrcClasses(module);
				ModuleUtils.addToList(targetClasses, classList);
			}
			this.targetClasses = targetClasses;
		}
	}

	/**
	 * Update the target tests (note we do not need to analyze the tests in
	 * dependent modules since they will be executed when the PraPR execution
	 * arrives at the corresponding module)
	 */
	public void updateTargetTests() {
		List<String> targetTests = getTargetTests();
		if (targetTests == null || targetTests.isEmpty()) {
			this.targetTests = ModuleUtils.getTestClasses(getProject());
		}
	}

	/**
	 * Check if the module is included in the targetmodules
	 *
	 * @param name
	 * @return True or False
	 */
	public boolean isInTargetModules(String name) {
		if (targetModules.size() == 0)
			return true;
		for (String targetModule : targetModules) {
			if (targetModule.equals(name))
				return true;
		}
		return false;
	}

	/**
	 * Check if the module should be excluded
	 *
	 * @param name
	 * @return True or False
	 */
	public boolean isInExcludedModules(String name) {
		if (excludedModules.size() == 0)
			return false;
		for (String excludedModule : excludedModules) {
			if (excludedModule.equals(name))
				return true;
		}
		return false;
	}

	/**
	 * Check if the current project has source root (note that all dependencies
	 * also need to be checked)
	 *
	 * @return True or False
	 */
	public Boolean hasCompileSourceRoots() {
		if (ModuleUtils.exists(getProject().getCompileSourceRoots()))
			return true;
		List<MavenProject> dependencies = ModuleUtils
				.getDependingModules(getProject());

		for (MavenProject dependency : dependencies) {
			if (ModuleUtils
					.exists(dependency.getCompileSourceRoots())) {
				return true;
			}
		}
		return false;
	}

	/**
	 * Check if the current project has test source root
	 *
	 * @return True or False
	 */
	public Boolean hasTestCompileSourceRoots() {
		return ModuleUtils
				.exists(getProject().getTestCompileSourceRoots());
	}

}