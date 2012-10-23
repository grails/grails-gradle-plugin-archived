/*
 * Copyright 2012 the original author or authors.
 *
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
 */

package org.grails.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.grails.launcher.*
import org.grails.launcher.context.GrailsLaunchContext
import org.grails.launcher.context.SerializableGrailsLaunchContext
import org.grails.launcher.version.GrailsVersion
import org.grails.launcher.version.GrailsVersionParser
import org.grails.launcher.util.NameUtils

class GrailsTask extends DefaultTask {

    String grailsVersion

    String grailsHome
    String command
    String args
    String env

    @InputFiles FileCollection providedClasspath
    @InputFiles FileCollection compileClasspath
    @InputFiles FileCollection runtimeClasspath
    @InputFiles FileCollection testClasspath
    @InputFiles FileCollection bootstrapClasspath

    boolean useRuntimeClasspathForBootstrap

    @Input
    boolean fork

    private projectDir
    private projectWorkDir

    GrailsTask() {
        System.setProperty("grails.console.enable.terminal", "false");
        System.setProperty("grails.console.enable.interactive", "false");
        command = name
    }

    @Input
    File getProjectDir() {
        projectDir == null ? null : project.file(projectDir)
    }

    void setProjectDir(projectDir) {
        this.projectDir = projectDir
    }

    @Input
    File getProjectWorkDir() {
        projectWorkDir == null ? null : project.file(projectWorkDir)
    }

    void setProjectWorkDir(projectWorkDir) {
        this.projectWorkDir = projectWorkDir
    }

    File getGrailsHome() {
        grailsHome == null ? null : project.file(grailsHome)
    }

    @TaskAction
    def executeCommand() {
        GrailsLauncher launcher = isFork() ? new ForkedGrailsLauncher() : new InProcessGrailsLauncher()
        int result = launcher.launch(createLaunchContext())
        if (result != 0) {
            throw new RuntimeException("[GrailsPlugin] Grails returned non-zero value: " + result);
        }
    }

    File getEffectiveGrailsHome() {
        getGrailsHome() ?: (project.hasProperty('grailsHome') ? project.file(project.grailsHome) : null)
    }

    protected GrailsVersion getParsedGrailsVersion() {
        new GrailsVersionParser().parse(getGrailsVersion() ?: project.grailsVersion)
    }

    boolean isEffectiveUseRuntimeClasspathForBootstrap() {
        getCommand() in ["run-app", "test-app", "release-plugin"] || isUseRuntimeClasspathForBootstrap()
    }

    protected void addToolsJarIfNecessary(Collection<File> classpath) {
        // Add the "tools.jar" to the classpath so that the Grails
        // scripts can run native2ascii. First assume that "java.home"
        // points to a JRE within a JDK.
        def javaHome = System.getProperty("java.home");
        def toolsJar = new File(javaHome, "../lib/tools.jar");
        if (!toolsJar.exists()) {
            // The "tools.jar" cannot be found with that path, so
            // now try with the assumption that "java.home" points
            // to a JDK.
            toolsJar = new File(javaHome, "tools.jar");
        }

        // There is no tools.jar, so native2ascii may not work. Note
        // that on Mac OS X, native2ascii is already on the classpath.
        if (!toolsJar.exists() && !System.getProperty('os.name') == 'Mac OS X') {
            project.logger.warn "[GrailsPlugin] Cannot find tools.jar in JAVA_HOME, so native2ascii may not work."
        }

        if (toolsJar.exists()) {
            classpath << toolsJar
        }
    }

    protected Collection<File> getEffectiveBootstrapClasspath() {
        def classpath = getBootstrapClasspath().files
        if (isEffectiveUseRuntimeClasspathForBootstrap()) {
            classpath.addAll(getRuntimeClasspath().files)
        }
        addToolsJarIfNecessary(classpath)
        classpath
    }

    protected GrailsLaunchContext createLaunchContext() {
        GrailsLaunchContext launchContext = new SerializableGrailsLaunchContext(getParsedGrailsVersion())

        launchContext.dependenciesExternallyConfigured = true
        launchContext.plainOutput = true

        launchContext.grailsHome = getEffectiveGrailsHome()
        launchContext.baseDir = getProjectDir()
        launchContext.env = getEnv()

        launchContext.scriptName = NameUtils.toScriptName(getCommand())
        launchContext.args = getArgs()

        launchContext.buildDependencies = getEffectiveBootstrapClasspath() as List
        launchContext.compileDependencies = getCompileClasspath().files as List
        launchContext.testDependencies = getTestClasspath().files as List
        launchContext.runtimeDependencies = getRuntimeClasspath().files as List

        // Provided deps are 2.0 only
        def providedClasspath = getProvidedClasspath().files as List
        if (providedClasspath) {
            try {
                launchContext.providedDependencies = providedClasspath
            } catch (NoSuchMethodException e) {
                throw new InvalidUserDataException("Cannot set provided classpath for task ${this} as this version of Grails does not support provided dependencies")
            }
        }

        launchContext.projectWorkDir = getProjectWorkDir()
        launchContext.classesDir = projectWorkDirFile("classes")
        launchContext.testClassesDir = projectWorkDirFile("test-classes")
        launchContext.resourcesDir = projectWorkDirFile("resources")
        launchContext.projectPluginsDir = projectWorkDirFile("plugins")
        launchContext.testReportsDir = projectWorkDirFile("test-results")

        launchContext
    }

    void logClasspaths() {
        project.logger.with {
            quiet "Classpath for Grails root loader:\n  ${getEffectiveBootstrapClasspath().join('\n  ')}"
            quiet "Compile classpath:\n  ${getCompileClasspath().files.join('\n  ')}"
            quiet "Test classpath:\n  ${getTestClasspath().files.join('\n  ')}"
            quiet "Runtime classpath:\n  ${getRuntimeClasspath().files.join('\n  ')}"
        }
    }

    protected boolean isPluginProject() {
        getProjectDir().listFiles({ dir, name -> name ==~ /.*GrailsPlugin.groovy/} as FilenameFilter) as boolean
    }

    protected File projectDirFile(String path) {
        new File(getProjectDir(), path)
    }

    protected File projectWorkDirFile(String path) {
        new File(getProjectWorkDir(), path)
    }
}
