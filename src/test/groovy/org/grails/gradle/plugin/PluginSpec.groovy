package org.grails.gradle.plugin

import spock.lang.Specification
import org.gradle.testfixtures.ProjectBuilder
import org.gradle.api.Project

class PluginSpec extends Specification {
    
    Project project = ProjectBuilder.builder().build()

    def setup() {
        project.grailsVersion = "2.0.0"
        project.apply plugin: "grails"
    }

    GrailsTask grailsTask(String name, Closure config = {}) {
        GrailsTask task = project.task(name, type: GrailsTask, config) as GrailsTask
    }
}
