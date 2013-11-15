package org.grails.gradle.plugin.tasks

/**
 * Creates a Grails application WAR file. By default this task is configured as a dependency of the 'assemble' task
 * when the Grails project is an application.
 */
class GrailsWarTask extends GrailsTask {

    GrailsWarTask() {
        command = 'war'
        description = 'Generates the application WAR file'
    }
}
