package org.grails.gradle.plugin.tasks

import org.gradle.api.Project
import org.grails.gradle.plugin.GrailsProject


class GrailsTaskConfigurator {

    void configure(Project project, GrailsProject grailsProject) {
        project.tasks.create("init", GrailsInitTask)

        def grailsClean = project.tasks.create("grails-clean", GrailsCleanTask)
        project.clean.dependsOn grailsClean

        def grailsAssemble = grailsProject.pluginProject ?
                project.tasks.create("grails-package-plugin", GrailsPackagePluginTask) :
                project.tasks.create("grails-war", GrailsWarTask)

        project.assemble.dependsOn grailsAssemble

        project.tasks.addRule("Grails command") { String name ->
            if (name.startsWith(GrailsTask.GRAILS_TASK_PREFIX)) {
                project.task(name, type: GrailsTask) {
                    command name - GrailsTask.GRAILS_TASK_PREFIX
                    if (project.hasProperty(GrailsTask.GRAILS_ARGS_PROPERTY)) {
                        args project.property(GrailsTask.GRAILS_ARGS_PROPERTY)
                    }
                    if (project.hasProperty(GrailsTask.GRAILS_ENV_PROPERTY)) {
                        env project.property(GrailsTask.GRAILS_ENV_PROPERTY)
                    }
                    if (project.hasProperty(GrailsTask.GRAILS_DEBUG_PROPERTY)) {
                        jvmOptions.debug = Boolean.parseBoolean(project.property(GrailsTask.GRAILS_DEBUG_PROPERTY))
                    }
                }
            }
        }
    }
}
