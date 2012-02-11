package org.grails.gradle.plugin

import spock.lang.Specification

class TaskConfigurationSpec extends PluginSpec {

    def "command defaults to task name"() {
        given:
        def task = grailsTask("compile")

        expect:
        task.command == "compile"

        when:
        task.command = "test"

        then:
        task.command == "test"
    }

    def "can log task classpath"() {
        given:
        def task = grailsTask("compile")
        task.compileClasspath = project.files("c")
        task.runtimeClasspath = project.files("r")
        task.testClasspath = project.files("t")
        task.bootstrapClasspath = project.files("b")
        task.bootstrapRuntimeClasspath = project.files("br")

        when:
        task.logClasspaths()

        then:
        notThrown(Exception)
    }
}
