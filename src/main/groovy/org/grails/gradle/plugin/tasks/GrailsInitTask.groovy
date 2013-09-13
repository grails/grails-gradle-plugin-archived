package org.grails.gradle.plugin.tasks

import org.gradle.api.InvalidUserDataException

class GrailsInitTask extends GrailsTask {

    GrailsInitTask() {
        onlyIf {
            !project.file("application.properties").exists() && !project.file("grails-app").exists()
        }

        doFirst {
            if (project.version == "unspecified") {
                throw new InvalidUserDataException("[GrailsPlugin] Build file must specify a 'version' property.")
            }
        }

        def projName = project.hasProperty(GRAILS_ARGS_PROPERTY) ? project.property(GRAILS_ARGS_PROPERTY) : project.projectDir.name

        command "create-app"
        args "--inplace --appVersion=$project.version $projName"
    }
}
