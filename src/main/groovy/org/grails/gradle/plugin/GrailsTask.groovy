package org.grails.gradle.plugin

import org.gradle.api.DefaultTask
import org.gradle.api.file.FileCollection
import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.TaskAction
import org.grails.launcher.GrailsLauncher
import org.grails.launcher.NameUtils
import org.grails.launcher.RootLoader

class GrailsTask extends DefaultTask {

    String grailsHome
    String command 
    String args
    String env
    
    @InputFiles FileCollection compileClasspath
    @InputFiles FileCollection runtimeClasspath
    @InputFiles FileCollection testClasspath
    @InputFiles FileCollection bootstrapClasspath
    @InputFiles FileCollection bootstrapRuntimeClasspath

    boolean useRuntimeClasspathForBootstrap

    private projectDir
    private targetDir
    
    GrailsTask() {
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
        def launchArgs = [NameUtils.toScriptName(command), args ?: ""]
        if (env) launchArgs << end
        def result = createLauncher().launch(*launchArgs)

        if (result != 0) {
            throw new RuntimeException("[GrailsPlugin] Grails returned non-zero value: " + result);
        }
    }

    String getEffectiveGrailsHome() {
        grailsHome ?: (project.hasProperty('grailsHome') ? project.grailsHome : null)
    }

    protected void verifyGrailsDependencies() {
        if (command == "create-app") return

        def runtimeDeps = project.configurations.runtime.resolvedConfiguration.resolvedArtifacts
        def grailsDep = runtimeDeps.find { it.resolvedDependency.moduleGroup == 'org.grails' && it.name.startsWith('grails-') }
        if (!grailsDep) {
            throw new RuntimeException("[GrailsPlugin] Your project does not contain any 'grails-*' dependencies in 'compile' or 'runtime'.")
        }

        def loggingDep = runtimeDeps.find { it.resolvedDependency.moduleGroup == 'org.slf4j' && it.name.startsWith('slf4j-') }
        if (!loggingDep) {
            throw new RuntimeException("[GrailsPlugin] Your project does not contain an SLF4J logging implementation dependency.")
        }
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
        command in ["run-app", "test-app", "release-plugin"] || useRuntimeClasspathForBootstrap
    }

    FileCollection getEffectiveBootstrapConfiguration() {
         effectiveUseRuntimeClasspathForBootstrap ? bootstrapRuntimeClasspath : bootstrapClasspath
    }

    protected Collection<URL> getEffectiveBootstrapClasspath() {
        def classpath = effectiveBootstrapConfiguration.files.collect { it.toURI().toURL() }
        addToolsJarIfNecessary(classpath)
        classpath
    }

    protected GrailsLauncher createLauncher() {
        def rootLoader = new RootLoader(getEffectiveBootstrapClasspath() as URL[], ClassLoader.systemClassLoader)
        def grailsLauncher = new GrailsLauncher(rootLoader, effectiveGrailsHome, getProjectDir().absolutePath)
        applyProjectLayout(grailsLauncher)
        configureGrailsDependencyManagement(grailsLauncher)
        grailsLauncher
    }

    protected void applyProjectLayout(GrailsLauncher grailsLauncher) {
        grailsLauncher.compileDependencies = compileClasspath.files as List
        grailsLauncher.testDependencies = testClasspath.files as List
        grailsLauncher.runtimeDependencies = runtimeClasspath.files as List
        grailsLauncher.projectWorkDir = project.buildDir
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

    protected void logClasspaths() {
        project.logger.with {
            if (infoEnabled) {
                info "Classpath for Grails root loader:\n  ${classpath.join('\n  ')}"
                info "Compile classpath:\n  ${project.configurations.compile.files.join('\n  ')}"
                info "Test classpath:\n  ${project.configurations.test.files.join('\n  ')}"
                info "Runtime classpath:\n  ${project.configurations.runtime.files.join('\n  ')}"
            }
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
