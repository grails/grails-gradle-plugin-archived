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

}
