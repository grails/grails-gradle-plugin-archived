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

package org.grails.gradle.plugin.tasks

import org.gradle.api.tasks.Delete
import org.grails.gradle.plugin.PluginSpec

/**
 * Created by Jeevanandam M. (jeeva@myjeeva.com) on 7/11/14.
 */
class GrailsEclipseJdtGroovyTaskSpec extends PluginSpec {

    def setup() {
        project.apply plugin: 'eclipse'
    }

    def 'test eclipse jdt groovy tasks are in place'() {
        given:
        def groovyJdtTasks = ['eclipseJdtGroovy', 'cleanEclipseJdtGroovy']

        expect:
        groovyJdtTasks.each {
            assert project.tasks.findByName(it)
        }
    }

    def 'test task type check'() {
        expect:
        project.tasks.findByName('eclipseJdtGroovy') instanceof GrailsEclipseJdtGroovyTask
        project.tasks.findByName('cleanEclipseJdtGroovy') instanceof Delete
    }

    def 'test eclipse jdt groovy prefs'() {
        when:
        project.tasks.getByName('eclipseJdtGroovy').execute()

        then:
        def eclipseJdtGroovy = project.tasks.findByName('eclipseJdtGroovy')
        def prefsFile = eclipseJdtGroovy.getOutputFile()
        def file = project.relativePath(prefsFile)
        def groovyCompilerVersion = eclipseJdtGroovy.getGroovyVersion()[0..2].replaceAll(/\./, '')

        assert file == '.settings/org.eclipse.jdt.groovy.core.prefs'
        assert prefsFile.text.contains("groovy.compiler.level=${groovyCompilerVersion}")
    }
}
