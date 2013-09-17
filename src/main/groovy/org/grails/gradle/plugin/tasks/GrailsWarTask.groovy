package org.grails.gradle.plugin.tasks

class GrailsWarTask extends GrailsTask {

    GrailsWarTask() {
        command = 'war'
        description = 'Generates the application WAR file'
    }
}
