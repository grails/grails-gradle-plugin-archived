package org.grails.gradle.plugin.tasks

class GrailsAssembleTask extends GrailsTask {

    File output
    File defaultOutput

    File getOutput() {
        if (this.@output == null) {
            return defaultOutput
        } else {
            return output
        }
    }
}
