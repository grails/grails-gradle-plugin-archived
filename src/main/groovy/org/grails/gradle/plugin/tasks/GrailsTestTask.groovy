package org.grails.gradle.plugin.tasks

import org.grails.launcher.context.GrailsLaunchContext

class GrailsTestTask extends GrailsTask {

    File testResultsDir

    private List<String> phases
    private String grailsArgs

    GrailsTestTask() {
        super()
        command = 'test-app'
        env = 'test'
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
    String getArgs() {
        [phases?.join(' '), grailsArgs, testSingle].findAll { it }.join(' ')
    }

    private String getTestSingle() {
        return System.getProperty('test.single', '')
    }
}
