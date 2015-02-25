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

package org.grails.gradle.plugin.tasks

import org.gradle.api.Action
import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.tasks.*
import org.gradle.process.ExecResult
import org.gradle.process.JavaForkOptions
import org.gradle.process.internal.DefaultJavaForkOptions
import org.gradle.process.internal.ExecException
import org.gradle.process.internal.JavaExecAction
import org.grails.gradle.plugin.internal.GrailsLaunchConfigureAction
import org.grails.launcher.context.GrailsLaunchContext
import org.grails.launcher.context.SerializableGrailsLaunchContext
import org.grails.launcher.util.NameUtils
import org.grails.launcher.version.GrailsVersion
import org.grails.launcher.version.GrailsVersionParser

/**
 * Base class for all Grails tasks
 */
class GrailsTask extends DefaultTask {

    static public final GRAILS_TASK_PREFIX = "grails-"
    static public final GRAILS_ARGS_PROPERTY = 'grailsArgs'
    static public final GRAILS_ENV_PROPERTY = 'grailsEnv'
    static public final GRAILS_DEBUG_PROPERTY = 'grailsDebug'
    static public final GRAILS_GROUP = 'grails'

    static public final String APP_GRAILS_VERSION = 'app.grails.version'
    static public final String APP_VERSION = 'app.version'

    String grailsVersion

    String grailsHome
    String command
    CharSequence args
    String env
    boolean reload

    @Optional @InputFiles FileCollection providedClasspath
    @Optional @InputFiles FileCollection compileClasspath
    @Optional @InputFiles FileCollection runtimeClasspath
    @Optional @InputFiles FileCollection testClasspath

    @Optional @InputFiles FileCollection springloaded

    @InputFiles FileCollection bootstrapClasspath

    boolean useRuntimeClasspathForBootstrap

    JavaForkOptions jvmOptions
    SourceSetContainer sourceSets

    private projectDir
    private projectWorkDir
    private boolean pluginProject

    boolean forwardStdIn
    boolean captureOutputToInfo

    GrailsTask() {
        this.jvmOptions = new DefaultJavaForkOptions(getServices().get(FileResolver))
        command = name
        group = GRAILS_GROUP
    }

    @Input
    File getProjectDir() {
        projectDir == null ? null : project.file(projectDir)
    }

    @Input
    File getProjectWorkDir() {
        projectWorkDir == null ? null : project.file(projectWorkDir)
    }

    File getGrailsHome() {
        grailsHome == null ? null : project.file(grailsHome)
    }

    public JavaForkOptions getJvmOptions() {
        return jvmOptions
    }

    public void jvmOptions(Action<JavaForkOptions> configure) {
        project.configure(jvmOptions, { configure.execute(it) })
    }

    @TaskAction
    def executeCommand() {
        handleVersionSync()
        def launchContext = createLaunchContext()
        def file = new File(getTemporaryDir(), "launch.context")

        def springloaded = getSpringloaded()
        def springloadedJar
        if (springloaded == null || !isReload()) {
            springloadedJar == null
        } else {
            springloadedJar = springloaded.singleFile
        }

        def launcher = new GrailsLaunchConfigureAction(launchContext, springloadedJar, file)

        // Capture output and only display to console in error conditions
        // if capture is enabled and info logging is not enabled.
        def capture = captureOutputToInfo && !logger.infoEnabled
        OutputStream out = capture ? new ByteArrayOutputStream() : System.out
        OutputStream err = capture ? new ByteArrayOutputStream() : System.err

        ExecResult result = project.javaexec {
            JavaExecAction action = delegate
            action.ignoreExitValue = true
            getJvmOptions().copyTo(action)
            if (forwardStdIn) {
                action.standardInput = System.in
            }
            action.standardOutput = out
            action.errorOutput = err
            launcher.execute(action)
        }

        try {
            checkExitValue(result)
        } catch (ExecException e) {
            if (capture) {
                if (out instanceof ByteArrayOutputStream) {
                    out.writeTo(System.out)
                }
                if (err instanceof ByteArrayOutputStream) {
                    err.writeTo(System.err)
                }
            }
            throw e
        }
    }

    protected void checkExitValue(ExecResult result) {
        result.rethrowFailure()
        result.assertNormalExitValue()
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
        launchContext.args = (getArgs() ? getArgs() + ' ' : '') + '--non-interactive'

        Iterable<File> files = getEffectiveBootstrapClasspath()
        if (files) {
            launchContext.buildDependencies = files as List
        }

        files = getCompileClasspath()
        if (files) {
            launchContext.compileDependencies = files as List
        }

        files = getTestClasspath()
        if (files) {
            launchContext.testDependencies = files as List
        }

        files = getRuntimeClasspath()
        if (files) {
            launchContext.runtimeDependencies = files as List
        }

        // Provided deps are 2.0 only
        files = getProvidedClasspath()
        if (files) {
            try {
                launchContext.providedDependencies = files as List
            } catch (NoSuchMethodException ignore) {
                throw new InvalidUserDataException("Cannot set provided classpath for task ${this} as this version of Grails does not support provided dependencies")
            }
        }

        launchContext.projectWorkDir = getProjectWorkDir()
        launchContext.classesDir = projectWorkDirFile("classes")
        launchContext.testClassesDir = projectWorkDirFile("test-classes")
        launchContext.resourcesDir = projectWorkDirFile("resources")
        launchContext.projectPluginsDir = projectDirFile("buildPlugins")
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

    private void handleVersionSync() {
        File appProperties = project.file('application.properties')
        URLClassLoader cl = new URLClassLoader(effectiveBootstrapClasspath.collect { it.toURI().toURL() } as URL[])
        Class metadataClass = cl.loadClass('grails.util.Metadata')
        Object metadata = metadataClass.newInstance(appProperties)
        if (syncVersions(metadata)) {
            metadata.persist()
        }
    }

    private boolean syncVersions(Object metadata) {
        boolean result = false

        Object appGrailsVersion = metadata.get(APP_GRAILS_VERSION)
        if (!getGrailsVersion().equals(appGrailsVersion)) {
            logger.info("updating ${APP_GRAILS_VERSION} to ${getGrailsVersion()}")
            metadata.put(APP_GRAILS_VERSION, this.getGrailsVersion())
            result = true
        }

        if (!isPluginProject()) {
            Object appVersion = metadata.get(APP_VERSION)
            if (appVersion != null) {
                if (!project.version.equals(appVersion)) {
                    logger.info("updating ${APP_VERSION} to ${project.version}")
                    metadata.put(APP_VERSION, project.version)
                    result = true
                }
            }
        } else {
            syncPluginVersion()
        }

        return result
    }

    // Reimplemented from https://github.com/grails/grails-core/blob/master/scripts/SetVersion.groovy
    private void syncPluginVersion() {
        File descriptor = project.grails.getPluginDescriptor()
        String content = descriptor.getText('UTF-8')
        def pattern = ~/def\s*version\s*=\s*"(.*)"/
        def matcher = (content =~ pattern)

        String newVersionString = "def version = \"${project.version}\""
        if (matcher.size() > 0) {
            content = content.replaceFirst(/def\s*version\s*=\s*".*"/, newVersionString)
        } else {
            content = content.replaceFirst(/\{/, "{\n\t$newVersionString // added by Gradle")
        }
        descriptor.withWriter('UTF-8') { it.write content }
    }
}
