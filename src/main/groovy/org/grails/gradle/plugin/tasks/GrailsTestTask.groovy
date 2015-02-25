package org.grails.gradle.plugin.tasks

import org.gradle.api.tasks.Input
import org.gradle.api.tasks.InputFiles
import org.gradle.api.tasks.Optional
import org.gradle.api.tasks.OutputDirectories
import org.gradle.api.tasks.OutputDirectory
import org.gradle.process.ExecResult
import org.grails.launcher.context.GrailsLaunchContext

/**
 * Executes Grails tests. By default this command will execute all the Grails test phases and types. A single test can
 * be executed by supplying '-Ptest.single=<test name>' when executing this task.
 *
 * Additionally, the test phases/types to executes can by configured by configuring the 'phases' property. This list
 * should contain the phases/types in the standard Grails format: ['unit:', 'integration:'], ['unit:spock']
 *
 * This tasks also tracks the Grails source as task inputs/outputs for up-to-date checking. If this behavior is
 * undesirably, configure the task with 'outputs.upToDateWhen { false }'.
 *
 * The plugin configures a task of this type (grails-test-app) as a dependency of the 'test' task.
 */
class GrailsTestTask extends GrailsTask {

    @OutputDirectory
    @Optional
    File testResultsDir

    private List<String> phases
    private String grailsArgs
    private boolean ignoreFailures = false;

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

    void setIgnoreFailures(boolean ignoreFailures) {
        this.ignoreFailures = ignoreFailures
    }

    @Override
    @Input
    String getArgs() {
        [phases?.join(' '), grailsArgs, testSingle].findAll { it }.join(' ')
    }

    String getTestSingle() {
        return System.getProperty('test.single', '')
    }

    @Override
    protected void checkExitValue(ExecResult result) {
        if(!ignoreFailures) {
            super.checkExitValue(result)
        }
    }
}
