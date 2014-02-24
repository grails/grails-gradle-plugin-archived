package org.grails.gradle.plugin.tasks

import org.gradle.api.tasks.OutputFile
import org.gradle.api.tasks.TaskInstantiationException

class GrailsAssembleTask extends GrailsTask {

    protected CharSequence output

    void setOutputFile(CharSequence file) {
        this.output = file
    }

    void setOutputFile(File file) {
        this.output = file.path
    }

    @OutputFile
    File getOutputFile() {
        return project.file(this.output)
    }
}

class GrailsPluginPackageTask extends GrailsAssembleTask {

    GrailsPluginPackageTask() {
        super.setOutputFile((CharSequence) "grails-${project.name}-${->project.version}.zip")
        command = 'package-plugin'
        description = 'Packages a grails plugin'
    }

    @Override
    void setOutputFile(CharSequence file) {
        throw new TaskInstantiationException("Assemble task for Grails Plugins is not configurable")
    }

    @Override
    void setOutputFile(File file) {
        throw new TaskInstantiationException("Assemble task for Grails Plugins is not configurable")
    }

}

class GrailsWarTask extends GrailsAssembleTask {

    GrailsWarTask() {
        super.setOutputFile((CharSequence) "build/distributions/${project.name}-${->project.version}.war")
        command = "war"
        description = 'Generates the application WAR file'
    }

    @Override
    CharSequence getArgs() {
        return "${-> output} ${-> super.args}"
    }

}
