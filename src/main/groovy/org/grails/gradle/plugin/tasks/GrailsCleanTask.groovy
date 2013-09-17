package org.grails.gradle.plugin.tasks

class GrailsCleanTask extends GrailsTask {

    GrailsCleanTask() {
        command = "clean"
        description = 'Executes Grails clean'
    }
}
