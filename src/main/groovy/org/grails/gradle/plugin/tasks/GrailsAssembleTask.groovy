package org.grails.gradle.plugin.tasks

import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskInstantiationException

class GrailsAssembleTask extends GrailsTask {

    @OutputFile
    private File output

    void setOutputFile(File file) {
        this.output = file
    }

    File getOutputFile() {
        return this.output
    }
}

class GrailsPluginPackageTask extends GrailsAssembleTask {

    GrailsPluginPackageTask() {
        super.setOutputFile(project.file("grails-${project.name}-${project.version}.zip"))
        command = 'package-plugin'
        description = 'Packages a grails plugin'
    }

    void setOutputFile(File file) {
        throw new TaskInstantiationException("Assemble task for Grails Plugins is not configurable")
    }

}

class GrailsWarTask extends GrailsAssembleTask {

    private String grailsArgs

    GrailsWarTask() {
        this.outputFile = project.file("build/distributions/${project.name}-${project.version}.war")
        command = "war"
        description = 'Generates the application WAR file'
    }

    @Override
    void setArgs(String args) {
        grailsArgs = args
    }

    @Override
    String getArgs() {
        return "${output.path} ${grailsArgs}"
    }

}
