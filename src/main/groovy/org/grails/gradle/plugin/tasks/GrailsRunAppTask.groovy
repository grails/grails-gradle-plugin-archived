package org.grails.gradle.plugin.tasks

/**
 * Startup the Grails application. This command will block until the Grails process is terminated.
 */
class GrailsRunAppTask extends GrailsTask {

    GrailsRunAppTask() {
        command = 'run-app'
        description = 'Starts the Grails application'
    }
}
