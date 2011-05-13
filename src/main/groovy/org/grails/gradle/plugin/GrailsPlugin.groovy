package org.grails.gradle.plugin

import grails.util.GrailsNameUtils
import org.gradle.api.Plugin
import org.gradle.api.Project

class GrailsPlugin implements Plugin<Project> {
    static public final GRAILS_TASK_PREFIX = "grails-"

    void apply(Project project) {
        if (!project.hasProperty("grailsVersion")) {
            throw new RuntimeException("[GrailsPlugin] the 'grailsVersion' project property is not set - you need to set this before applying the plugin")
        }
        
        project.configurations {
            logging
            
            compile.extendsFrom logging
            runtime.extendsFrom compile
            test.extendsFrom compile
            
            bootstrap.extendsFrom logging
            bootstrapRuntime.extendsFrom bootstrap, runtime
        }

        // Set up the 'bootstrap' configuration so that it contains
        // all the dependencies required by the Grails build system. This
        // pretty much means everything used by the scripts too.
        project.dependencies {
            bootstrap "org.grails:grails-bootstrap:${project.grailsVersion}",
                      "org.grails:grails-scripts:${project.grailsVersion}",
                      "org.apache.ivy:ivy:2.1.0"
        }
        
        // Provide a task that allows the user to create a fresh Grails
        // project from a basic Gradle build file.
        project.task("init") << {
            // First make sure that a project version has been configured.
            if (project.version == "unspecified") {
                throw new RuntimeException("[GrailsPlugin] Build file must specify a 'version' property.")
            }

            // Don't create a new project if one already exists.
            if (project.file("application.properties").exists() && project.file("grails-app").exists()) {
                logger.warn "Grails project already exists - SKIPPING"
                return
            }

            // The project name comes from the name of the project
            // directory, but this can be overridden by an argument.
            def projName = project.hasProperty("args") ? project.args : project.projectDir.name
            GrailsTask.runGrails("CreateApp", project, "--inplace --appVersion=" + project.version + " " + projName)
        }

        // Make the Grails 'clean' command available as a 'clean' task.
        project.task("clean", overwrite: true) << {
            GrailsTask.runGrailsWithProps("Clean", project)
        }
        addDependencyToProjectLibTasks(project.clean)

        // Most people are used to a "test" target or task, but Grails
        // has "test-app". So we hard-code a "test" task.
        project.task(overwrite: true, "test") << {
            GrailsTask.runGrailsWithProps("TestApp", project)
        }
        addDependencyToProjectLibTasks(project.test)

        // Gradle's Java plugin provides an "assemble" task. We map that
        // to the War command here for applications and PackagePlugin for
        // Grails plugins.
        project.task(overwrite: true, "assemble") << {
            if (project.projectDir.listFiles({ dir, name -> name ==~ /.*GrailsPlugin.groovy/} as FilenameFilter)) {
                GrailsTask.runGrailsWithProps("PackagePlugin", project)
            }
            else {
                GrailsTask.runGrailsWithProps("War", project)
            }
        }
        addDependencyToProjectLibTasks(project.assemble)
        
        // Attach a generic “grailsTask” method so the build can run arbitrary tasks
        project.grailsTask = { String cmd, String args = null, String env = null ->
            GrailsTask.runGrails(GrailsNameUtils.getNameFromScript(cmd), project, args, env)
        }

        // Convert any task executed from the command line 
        // with the special prefix into the Grails equivalent command.
        project.gradle.afterProject { p, ex ->
            if (p == project) { // Only add the task to the project that applied the plugin
                project.tasks.addRule("Grails command") { String name ->
                    if (name.startsWith(GRAILS_TASK_PREFIX)) {
                        // Add a task for the given Grails command.
                        project.task(name) << {
                            GrailsTask.runGrailsWithProps(GrailsNameUtils.getNameFromScript(name - GRAILS_TASK_PREFIX), project)
                        }
                        addDependencyToProjectLibTasks(project."$name")
                    }
                }
            }
        }
    }

    /**
     * Evaluate any project lib dependencies in any of the configurations
     * and add the jar task of that project as a dependency of the task we created
     */
    private void addDependencyToProjectLibTasks(task) {
        task.project.configurations.each {
            task.dependsOn(it.getTaskDependencyFromProjectDependency(true, "jar"))
        }
    }
}
