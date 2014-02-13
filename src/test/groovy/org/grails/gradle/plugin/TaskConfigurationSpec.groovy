/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.grails.gradle.plugin

import org.gradle.testfixtures.ProjectBuilder

class TaskConfigurationSpec extends PluginSpec {

    def "basic tasks are in place"() {
        given:
        def baseTasks = ['test', 'check', 'build', 'assemble', 'clean']
        def grailsTasks = ['init', 'init-plugin', 'grails-clean', 'grails-test-app', 'grails-run-app', 'grails-war']

        expect:
        (baseTasks + grailsTasks).each {
            assert project.tasks.findByName(it)
        }
    }

    def "default configuration extends the runtime configuration"() {
        expect:
        project.configurations.default.extendsFrom.contains(project.configurations.runtime)
    }

    def "war file is configured as runtime artifact for application"() {
        given:
        List<File> artifactFiles = project.configurations.runtime.artifacts.files.files as List

        expect:
        assert artifactFiles.size() == 1
        assert artifactFiles.first() == project.file("build/distributions/${project.name}-${project.version}.war")

        and:
        assert project.configurations.default.allArtifacts.files.files.toList().first() ==
                project.file("build/distributions/${project.name}-${project.version}.war")
    }

    def "zip file is configured as runtime artifact for plugin"() {
        given:
        project = ProjectBuilder.builder().build()

        project.file("${project.name.capitalize()}GrailsPlugin.groovy") << """
class ${project.name.capitalize()}GrailsPlugin { }
"""
        project.grailsVersion = "2.0.0"
        project.apply plugin: "grails"
        List<File> artifactFiles = project.configurations.runtime.artifacts.files.files as List

        expect:
        assert artifactFiles.size() == 1
        assert artifactFiles.first() == project.file("grails-${project.name}-${project.version}.zip")

        and:
        assert project.configurations.default.allArtifacts.files.files.toList().first() ==
                project.file("grails-${project.name}-${project.version}.zip")

        and:
        assert project.tasks.findByName('grails-package-plugin')
    }

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
