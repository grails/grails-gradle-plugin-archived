package org.grails.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.InvalidUserDataException
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.grails.launcher.*

class GrailsTask extends DefaultTask {

    String grailsHome
    String command
    String args
    String env

    @InputFiles FileCollection providedClasspath
    @InputFiles FileCollection compileClasspath
    @InputFiles FileCollection runtimeClasspath
    @InputFiles FileCollection testClasspath
    @InputFiles FileCollection bootstrapClasspath
    @InputFiles FileCollection bootstrapRuntimeClasspath

    boolean useRuntimeClasspathForBootstrap

    @Input
    boolean fork

    private projectDir
    private targetDir

    GrailsTask() {
        System.setProperty("grails.console.enable.terminal", "false");
        System.setProperty("grails.console.enable.interactive", "false");
        command = name
    }

    @Input
    File getProjectDir() {
        project.file(projectDir)
    }

    void setProjectDir(projectDir) {
        this.projectDir = projectDir
    }

    @Input
    File getTargetDir() {
        project.file(targetDir)
    }

    void setTargetDir(targetDir) {
        this.targetDir = targetDir
    }

    @TaskAction
    def executeCommand() {
        def result = 0
        if (fork) {
            ForkedGrailsLauncher.ExecutionContext ec = new ForkedGrailsLauncher.ExecutionContext();

            ec.setBuildDependencies(getBootstrapClasspath().files.toList());
            ec.setProvidedDependencies(getProvidedClasspath().files as List);
            ec.setCompileDependencies(getCompileClasspath().files as List);
            ec.setTestDependencies(getTestClasspath().files as List);
            ec.setRuntimeDependencies(getRuntimeClasspath().files as List);
            ec.setGrailsWorkDir(new File(getTargetDir(), "grails-work"));
            ec.setProjectWorkDir(getTargetDir());
            ec.setClassesDir(new File(getTargetDir(), "classes"));
            ec.setTestClassesDir(new File(getTargetDir(), "test-classes"));
            ec.setResourcesDir(new File(getTargetDir(), "resources"));
            ec.setProjectPluginsDir(new File(getTargetDir(), "plugins"));
            ec.setArgs(getArgs());
            ec.setScriptName(getCommand());
            ec.setBaseDir(getProjectDir());
            ec.setEnv(getEnv());
            ForkedGrailsLauncher fgr = new ForkedGrailsLauncher(ec);
            try {
                fgr.fork();
            } catch (Exception e) {
                throw new RuntimeException(e.getMessage(), e);
            }
        }
        else {

            def launchArgs = [NameUtils.toScriptName(getCommand()), getArgs() ?: ""]
            if (getEnv()) launchArgs << getEnv()
            try {
                result = createLauncher().launch(* launchArgs)
            } catch (e) {
                while (e.cause != null && e.cause != e) {
                    e = e.cause
                }
                if (e.class.name.contains("ScriptNotFound")) {
                    throw new RuntimeException("[GrailsPlugin] Grails command $launchArgs not found");
                }
                else {
                    throw e
                }
            }
        }

        if (result != 0) {
            throw new RuntimeException("[GrailsPlugin] Grails returned non-zero value: " + result);
        }
    }

    String getEffectiveGrailsHome() {
        grailsHome ?: (project.hasProperty('grailsHome') ? project.grailsHome : null)
    }

    protected void addToolsJarIfNecessary(Collection<URL> classpath) {
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
            classpath << toolsJar.toURI().toURL()
        }
    }

    boolean isEffectiveUseRuntimeClasspathForBootstrap() {
        getCommand() in ["run-app", "test-app", "release-plugin"] || isUseRuntimeClasspathForBootstrap()
    }

    FileCollection getEffectiveBootstrapConfiguration() {
        effectiveUseRuntimeClasspathForBootstrap ? getBootstrapRuntimeClasspath() : getBootstrapClasspath()
    }

    protected Collection<URL> getEffectiveBootstrapClasspath() {
        def classpath = getEffectiveBootstrapConfiguration().files.collect { it.toURI().toURL() }
        addToolsJarIfNecessary(classpath)
        classpath
    }

    protected GrailsLauncher createLauncher() {
        def rootLoader = new RootLoader(getEffectiveBootstrapClasspath() as URL[], ClassLoader.systemClassLoader)
        def grailsLauncher = new GrailsLauncher(rootLoader, getEffectiveGrailsHome(), getProjectDir().absolutePath)
        applyProjectLayout(grailsLauncher)
        configureGrailsDependencyManagement(grailsLauncher)
        BootstrapUtils.addLoggingJarsToRootLoaderAndInit(rootLoader, getCompileClasspath().files as List)
        grailsLauncher
    }

    protected void applyProjectLayout(GrailsLauncher grailsLauncher) {

        // Provided deps are 2.0 only
        def providedClasspath = getProvidedClasspath().files as List
        if (providedClasspath) {
            try {
                grailsLauncher.providedDependencies = providedClasspath
            } catch (NoSuchMethodException e) {
                throw new InvalidUserDataException("Cannot set provided classpath for task ${this} as this version of Grails does not support provided dependencies")
            }
        }

        grailsLauncher.compileDependencies = getCompileClasspath().files as List
        grailsLauncher.testDependencies = getTestClasspath().files as List
        grailsLauncher.runtimeDependencies = getRuntimeClasspath().files as List
        grailsLauncher.projectWorkDir = getTargetDir()
        grailsLauncher.classesDir = targetFile("classes")
        grailsLauncher.testClassesDir = targetFile("test-classes")
        grailsLauncher.resourcesDir = targetFile("resources")
        grailsLauncher.projectPluginsDir = targetFile("plugins")
        grailsLauncher.testReportsDir = targetFile("test-results")
    }

    protected void configureGrailsDependencyManagement(GrailsLauncher grailsLauncher) {
        // Grails 1.2+ only. Previous versions of Grails don't have the
        // 'dependenciesExternallyConfigured' property. Note that this
        // is a HACK because the 'settings' field is private.
        //
        // We can't simply check whether the property exists on the
        // launcher because it's the 1.2 version, whereas the project may
        // be using Grails version 1.1. That's why we have to get hold
        // of the actual BuildSettings instance.
        def buildSettings = grailsLauncher.settings
        if (buildSettings.metaClass.hasProperty(buildSettings, "dependenciesExternallyConfigured")) {
            grailsLauncher.dependenciesExternallyConfigured = true
        }
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

    protected File projectFile(String path) {
        new File(getProjectDir(), path)
    }

    protected File targetFile(String path) {
        new File(getTargetDir(), path)
    }
}
