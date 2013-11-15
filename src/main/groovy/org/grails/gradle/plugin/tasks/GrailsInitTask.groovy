package org.grails.gradle.plugin.tasks

import org.gradle.api.InvalidUserDataException
import org.gradle.api.tasks.TaskAction

/**
 * Creates a new Grails application in the working directory. This requires that the 'project.version' property
 * be configured in the Gradle build.
 *
 * By default this will create the Grails application with a name matching the projectDir name. This can be overridden
 * on the command line by supplying '-PgrailsArgs=<project name>' when executing the command
 */
class GrailsInitTask extends GrailsTask {

    GrailsInitTask() {
        onlyIf {
            !project.file("application.properties").exists() && !project.file("grails-app").exists()
        }

        def projName = project.hasProperty(GRAILS_ARGS_PROPERTY) ? project.property(GRAILS_ARGS_PROPERTY) : project.projectDir.name

        command = "create-app"
        args = "--inplace --appVersion=$project.version $projName"
        description = 'Creates a new Grails application in the current directory'
    }

    @TaskAction
    def executeCommand() {
        if (project.version == "unspecified") {
            throw new InvalidUserDataException("[GrailsPlugin] Build file must specify a 'version' property.")
        }
        super.executeCommand()
    }
}
