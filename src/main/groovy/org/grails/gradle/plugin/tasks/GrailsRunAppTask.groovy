package org.grails.gradle.plugin.tasks

class GrailsRunAppTask extends GrailsTask {

    GrailsRunAppTask() {
        command = 'run-app'
        description = 'Starts the Grails application'
    }
}
