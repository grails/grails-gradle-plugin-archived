package org.grails.gradle.plugin

import org.gradle.api.Plugin
import org.gradle.api.Project

class GrailsPlugin implements Plugin<Project> {
    static public final GRAILS_TASK_PREFIX = "grails-"

    void apply(Project project) {
        if (!project.hasProperty("grailsVersion")) {
            throw new RuntimeException("[GrailsPlugin] the 'grailsVersion' project property is not set - you need to set this before applying the plugin")
        }

        String grailsVersion = project.grailsVersion

        project.configurations {
            compile
            runtime.extendsFrom compile
            test.extendsFrom compile

            bootstrap.extendsFrom logging
            bootstrapRuntime.extendsFrom bootstrap, runtime
        }

        project.dependencies {
            ["bootstrap", "scripts", "resources"].each {
                bootstrap("org.grails:grails-$it:${project.grailsVersion}") {
                    exclude group: "org.slf4j"
                }
            }

            if (grailsVersion.startsWith("1.3")) {
                bootstrap "org.apache.ant:ant:1.8.2"
            }

            if (!grailsVersion.startsWith("2")) {
                bootstrap "org.apache.ivy:ivy:2.1.0"
            }
        }

        project.task("init", type: GrailsTask) {
            onlyIf {
                !project.file("application.properties").exists() && !project.file("grails-app").exists()
            }

            doFirst {
                if (project.version == "unspecified") {
                    throw new RuntimeException("[GrailsPlugin] Build file must specify a 'version' property.")
                }
            }

            def projName = project.hasProperty("args") ? project.args : project.projectDir.name

            command "create-app"
            args "--inplace --appVersion=$project.version $projName"
        }

        project.task("clean", type: GrailsTask, overwrite: true)

        project.task("test", type: GrailsTask, overwrite: true) {
            command "test-app"
            configurations "compile", "test"
        }

        project.task("assemble", type: GrailsTask, overwrite: true) {
            command pluginProject ? "package-plugin" : "war"
            configuration "compile"
        }

        // Convert any task executed from the command line
        // with the special prefix into the Grails equivalent command.
        project.gradle.afterProject { p, ex ->
            if (p == project) {
                project.tasks.addRule("Grails command") { String name ->
                    if (name.startsWith(GRAILS_TASK_PREFIX)) {
                        project.task(name, type: GrailsTask) {
                            command name - GRAILS_TASK_PREFIX

                            // We don't really know what configurations are necessary, but compile is a good default
                            configuration "compile"
                        }
                    }
                }
            }
        }
    }
}
