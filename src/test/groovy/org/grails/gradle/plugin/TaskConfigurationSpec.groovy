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
