package org.grails.gradle.plugin.tasks

import org.grails.launcher.context.GrailsLaunchContext

class GrailsTestTask extends GrailsTask {

    File testResultsDir

    @Override
    protected GrailsLaunchContext createLaunchContext() {
        GrailsLaunchContext ctx = super.createLaunchContext()
        if (testResultsDir) {
            ctx.testReportsDir = testResultsDir
        }
        return ctx
    }
}
