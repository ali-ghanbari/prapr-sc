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
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import org.apache.maven.artifact.Artifact;
import org.apache.maven.project.MavenProject;
import org.pitest.classinfo.ClassName;
import org.pitest.classpath.DirectoryClassPathRoot;
import org.pitest.functional.F;
import org.pitest.functional.FCollection;

/**
 * @author Lingming Zhang (lingming.zhang@utdallas.edu)
 * @since 2.0.1
 */
public class ModuleUtils
{

	/**
	 * Maintain the list of modules within the root dir
	 */
	static List<MavenProject> modules;

	/**
	 * Add from sourceList to targetList without duplication
	 * 
	 * @param targetList
	 * @param sourceList
	 */
	public static void addToList(List<String> targetList,
			List<String> sourceList) {
		if (sourceList == null)
			return;

		for (String cur : sourceList) {
			if (!targetList.contains(cur)) {
				targetList.add(cur);
			}
		}
	}

	/**
	 * Transform names to files
	 * 
	 * @param nameList
	 * @return
	 */
	public static List<File> namesToFiles(List<String> nameList) {
		List<File> files = new ArrayList<File>();
		F<String, File> map = new F<String, File>() {
			@Override
			public File apply(final String s) {
				File f = new File(s);
				if (f.exists())
					return f;
				else
					return null;
			}
		};
		for (String name : nameList) {
			File file = map.apply(name);
			if (file != null)
				files.add(file);
		}
		return files;
	}

	/**
	 * Check if any file exist
	 * 
	 * @param fileNameList
	 * @return True or False
	 */
	public static Boolean exists(List<String> fileNameList) {
		for (String name : fileNameList) {
			File file = new File(name);
			if (file.exists())
				return true;
		}
		return false;
	}

	/**
	 * Get the source classes for a given project
	 * 
	 * @param project
	 * @return The list of source classes
	 */
	public static List<String> getSrcClasses(MavenProject project) {
		return getClassPatternsFromDir(project.getBuild().getOutputDirectory());
	}

	/**
	 * Get the test classes for a given project
	 * 
	 * @param project
	 * @return The list of test classes
	 */
	public static ArrayList<String> getTestClasses(MavenProject project) {
		return getClassPatternsFromDir(
				project.getBuild().getTestOutputDirectory());
	}

	/**
	 * Get all class patterns from a dir
	 * 
	 * @param buildOutputDirectory
	 * @return The list of class patterns
	 */
	private static ArrayList<String> getClassPatternsFromDir(
			String buildOutputDirectory) {
		Set<String> classPatterns = new HashSet<String>();
		F<String, String> classToPatterns = new F<String, String>() {
			@Override
			public String apply(String clazz) {
				return ClassName.fromString(clazz).getPackage().asJavaName()
						+ ".*";
			}
		};
		File outputDir = new File(buildOutputDirectory);
		if (outputDir.exists()) {
			DirectoryClassPathRoot classRoot = new DirectoryClassPathRoot(
					outputDir);
			Collection<String> classes = classRoot.classNames();
			FCollection.mapTo(classes, classToPatterns, classPatterns);
		}
		return new ArrayList<String>(classPatterns);
	}

	/**
	 * Initialize the module list for the current project under analysis
	 * 
	 * @param mojo
	 */
	public static void initialize(PraPRMultiMojo mojo) {
		List<MavenProject> moduleList = mojo.getProject()
				.getCollectedProjects();
		if (modules == null) {
			modules = moduleList;
		}
	}

	/**
	 * Return the dependent modules for a project
	 * 
	 * @param project
	 * @return The list of dependent modules
	 */
	public static List<MavenProject> getDependingModules(MavenProject project) {
		List<MavenProject> modules = new ArrayList<MavenProject>();
		Set<Artifact> dependencies = project.getArtifacts();

		for (Artifact dependency : dependencies) {
			MavenProject moduleProject = getMavenProjectFromName(
					dependency.getArtifactId());
			if (moduleProject != null) {
				modules.add(moduleProject);
			}
		}
		return modules;
	}

	/**
	 * Check if a project is a sub-module of the current project
	 * 
	 * @param name
	 * @return True of False
	 */
	public static boolean isProjectModule(String name) {
		for (MavenProject module : modules) {
			if (module.getArtifactId().equals(name))
				return true;
		}
		return false;
	}

	/**
	 * Get mavenproject from name
	 * 
	 * @param name
	 * @return MavenProject
	 */
	public static MavenProject getMavenProjectFromName(String name) {
		for (MavenProject module : modules) {
			if (module.getArtifactId().equals(name)) {
				return module;
			}
		}
		return null;
	}

}