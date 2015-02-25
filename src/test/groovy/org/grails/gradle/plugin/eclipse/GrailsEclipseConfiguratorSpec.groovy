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

package org.grails.gradle.plugin.eclipse

import org.gradle.plugins.ide.eclipse.model.BuildCommand
import org.grails.gradle.plugin.PluginSpec

/**
 * Created by Jeevanandam M. (jeeva@myjeeva.com) on 7/11/14.
 */
class GrailsEclipseConfiguratorSpec extends PluginSpec {

    def setup() {
        project.apply plugin: 'eclipse'
    }

    def 'test eclipse project creation'() {
        given:
        def testNatures = ['org.grails.ide.eclipse.core.nature', 'org.eclipse.jdt.groovy.core.groovyNature',
                           'org.eclipse.jdt.core.javanature', 'org.eclipse.wst.common.project.facet.core.nature']

        def testBuildCommands = []
        testBuildCommands << new BuildCommand('org.eclipse.wst.common.project.facet.core.builder')
        testBuildCommands << new BuildCommand('org.eclipse.jdt.core.javabuilder')

        expect:
        assert project.eclipse.project.natures == testNatures
        assert project.eclipse.project.buildCommands == testBuildCommands
    }

    def "eclipse classpath source and test source configured"() {
        given:
        def sourceSets = project.eclipse.classpath.sourceSets.collect{ it }
        def sourceSet = sourceSets.get(0)
        def testSourceSet = sourceSets.get(1)

        expect:
        [
            'src/groovy', 'src/java', 'grails-app/controllers',
            'grails-app/domain', 'grails-app/services', 'grails-app/taglib'
        ].each {
            def directory = project.file(it)
            assert sourceSet.allSource.srcDirs.contains(directory)
        }

        and:
        [
            'test/unit', 'test/integration', 'test/functional'
        ].each {
            def directory = project.file(it)
            assert testSourceSet.allSource.srcDirs.contains(directory)
        }
    }


    def 'eclipse classpath libraries configured'() {
        given:
        def dependencies = [project.configurations.bootstrap] +
            [project.configurations.runtime] + [project.configurations.test]

        expect:
        assert project.eclipse.classpath.plusConfigurations == dependencies
    }
}
