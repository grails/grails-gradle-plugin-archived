package org.grails.gradle.plugin.tasks

/**
 * Task for executing the Grails clean command.
 */
class GrailsCleanTask extends GrailsTask {

    GrailsCleanTask() {
        command = "clean"
        description = 'Executes Grails clean'
    }
}
