package org.grails.gradle.plugin.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputFile
import org.grails.launcher.context.GrailsLaunchContext

class GrailsTestTask extends GrailsTask {

    @OutputFile
    @Optional
    File testResultsDir

    private List<String> phases
    private String grailsArgs

    GrailsTestTask() {
        super()
        command = 'test-app'
        env = 'test'
        description = 'Executes Grails tests'
    }

    @InputFiles
    Set<File> getSourceInputs() {
        sourceSets.getByName('main').allSource.files + sourceSets.getByName('test').allSource.files
    }

    @OutputDirectories
    Set<File> getSourceOutputs() {
        [sourceSets.getByName('main').output.classesDir, sourceSets.getByName('test').output.classesDir]
    }

    @Override
    protected GrailsLaunchContext createLaunchContext() {
        GrailsLaunchContext ctx = super.createLaunchContext()
        if (testResultsDir) {
            ctx.testReportsDir = testResultsDir
        }
        return ctx
    }

    void setPhases(List<String> phases) {
        this.phases = phases
    }

    void setArgs(String args) {
        grailsArgs = args
    }

    @Override
    @Input
    String getArgs() {
        [phases?.join(' '), grailsArgs, testSingle].findAll { it }.join(' ')
    }

    String getTestSingle() {
        return System.getProperty('test.single', '')
    }
}
