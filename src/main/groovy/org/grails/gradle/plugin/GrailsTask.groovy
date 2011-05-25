package org.grails.gradle.plugin

import grails.util.GrailsNameUtils

import org.codehaus.groovy.grails.cli.support.GrailsRootLoader
import org.codehaus.groovy.grails.cli.support.GrailsBuildHelper
import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.tasks.TaskAction

class GrailsTask extends DefaultTask {
    /**
     * These Grails commands require the project's runtime dependencies
     * in the Grails root loader because they are not using the runtime
     * classpath (as they are supposed to).
     */
    static final RUNTIME_CLASSPATH_COMMANDS = [ "RunApp", "TestApp" ] as Set

    String command
    String args
    String env

    @TaskAction
    def executeCommand() {
        runGrails(GrailsNameUtils.getNameFromScript(cmd), project, args, env)
    }
    
    /**
     * Launches Grails and executes the given command. Any command
     * arguments or environment are picked up from the "args" and "env"
     * project properties.
     * @param cmd The Grails command to execute. Note that this should
     * actually be the name of the script, not the command name. So
     * "RunApp" rather than "run-app".
     * @param project The Gradle project to run Grails in.
     */
    protected static void runGrailsWithProps(String cmd, Project project) {
        def cmdArgs = project.hasProperty("args") ? project.args : null
        def cmdEnv = project.hasProperty("env") ? project.env : null
        runGrails(cmd, project, cmdArgs, cmdEnv)
    }

    /**
     * Launches Grails and executes the given command.
     * @param cmd The Grails command to execute. Note that this should
     * actually be the name of the script, not the command name. So
     * "RunApp" rather than "run-app".
     * @param project The Gradle project to run Grails in.
     * @param args (Optional) Any arguments (as a single, space-separated
     * string) that you want to pass to the Grails command. Defaults to
     * <code>null</code> (no args).
     * @param env (Optional) The environment to run the Grails command
     * in. Defaults to <code>null</code>, which means that the command
     * uses whatever its default environment is.
     */
    protected static void runGrails(String cmd, Project project, String args = null, String env = null) {
        // Start by checking that the project has both Grails and a
        // logging implementation as dependencies. Otherwise we fail
        // the build.
        def runtimeDeps = project.configurations.runtime.resolvedConfiguration.resolvedArtifacts
        def grailsDep = runtimeDeps.find { it.resolvedDependency.moduleGroup == 'org.grails' && it.name.startsWith('grails-') }
        if (!grailsDep) {
            throw new RuntimeException("[GrailsPlugin] Your project does not contain any 'grails-*' dependencies in 'compile' or 'runtime'.")
        }

        def loggingDep = runtimeDeps.find { it.resolvedDependency.moduleGroup == 'org.slf4j' && it.name.startsWith('slf4j-') }
        if (!loggingDep) {
            throw new RuntimeException("[GrailsPlugin] Your project does not contain an SLF4J logging implementation dependency.")
        }

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
        
        def bootsrapConfiguration = cmd in RUNTIME_CLASSPATH_COMMANDS ? project.configurations.bootstrapRuntime : project.configurations.bootstrap
        
        // Get the bootstrap configuration as a list of URLs
        // and add tools.jar to it.
        def classpath = bootsrapConfiguration.files.collect { it.toURI().toURL() }
        classpath << toolsJar.toURI().toURL()

        // So we know what files are on what classpaths.
        project.logger.info "Classpath for Grails root loader:\n  ${classpath.join('\n  ')}"
        project.logger.info "Compile classpath:\n  ${project.configurations.compile.files.join('\n  ')}"
        project.logger.info "Test classpath:\n  ${project.configurations.test.files.join('\n  ')}"
        project.logger.info "Runtime classpath:\n  ${project.configurations.runtime.files.join('\n  ')}"

        // Finally, kick off Grails with the given command. GrailsBuildHelper
        // allows us to easily configure the Grails build settings and the
        // various lists of dependencies. It also ensures that the Grails
        // build system runs in its own class loader so that Gradle's
        // dependencies don't conflict with it.
        def rootLoader = new GrailsRootLoader(classpath as URL[], ClassLoader.systemClassLoader)
        def grailsHelper = new GrailsBuildHelper(rootLoader, null, project.projectDir.absolutePath)
        grailsHelper.compileDependencies = project.configurations.compile.files as List
        grailsHelper.testDependencies = project.configurations.test.files as List
        grailsHelper.runtimeDependencies = project.configurations.runtime.files as List
        grailsHelper.projectWorkDir = project.buildDir
        grailsHelper.classesDir = new File(project.buildDir, "classes")
        grailsHelper.testClassesDir = new File(project.buildDir, "test-classes")
        grailsHelper.resourcesDir = new File(project.buildDir, "resources")
        grailsHelper.projectPluginsDir = new File(project.buildDir, "plugins")
        grailsHelper.testReportsDir = new File(project.buildDir, "test-results")

        // Grails 1.2+ only. Previous versions of Grails don't have the
        // 'dependenciesExternallyConfigured' property. Note that this
        // is a HACK because the 'settings' field is private.
        //
        // We can't simply check whether the property exists on the
        // helper because it's the 1.2 version, whereas the project may
        // be using Grails version 1.1. That's why we have to get hold
        // of the actual BuildSettings instance.
        def buildSettings = grailsHelper.settings
        if (buildSettings.metaClass.hasProperty(buildSettings, "dependenciesExternallyConfigured")) {
            grailsHelper.dependenciesExternallyConfigured = true
        }
        
        // Using a Groovy trick here, because we either want to call
        // the execute() method that takes two arguments, or the one
        // that takes three. So rather than calling those methods
        // explicitly within a condition, we create an argument list
        // and then use the spread operator.
        def methodArgs = [ cmd, args ]
        if (env) methodArgs << env

        def retval = grailsHelper.execute(*methodArgs)
        if (retval != 0) {
            throw new RuntimeException("[GrailsPlugin] Grails returned non-zero value: " + retval);
        }
    }
}
