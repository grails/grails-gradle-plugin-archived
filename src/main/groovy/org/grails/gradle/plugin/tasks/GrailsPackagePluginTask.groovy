package org.grails.gradle.plugin.tasks

class GrailsPackagePluginTask extends GrailsTask {

    GrailsPackagePluginTask() {
        command = 'package-plugin'
        description = 'Packages a grails plugin'
    }
}
