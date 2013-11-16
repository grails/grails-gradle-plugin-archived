package org.grails.gradle.plugin.tasks

/**
 * Packages a Grails plugin project. By default this task is configured as a dependency to the 'assemble' task if
 * the Grails project is a plugin project.
 */
class GrailsPackagePluginTask extends GrailsTask {

    GrailsPackagePluginTask() {
        command = 'package-plugin'
        description = 'Packages a grails plugin'
    }
}
