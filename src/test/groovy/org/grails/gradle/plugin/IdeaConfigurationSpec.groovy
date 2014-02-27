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

class IdeaConfigurationSpec extends PluginSpec {

    def setup() {
        project.apply plugin: 'idea'
    }

    def "idea module has scopes configured"() {
        expect:
        project.idea.module.scopes.keySet() == ['PROVIDED', 'COMPILE', 'RUNTIME', 'TEST'] as Set

        and:
        project.configurations.provided in project.idea.module.scopes.PROVIDED.plus
        project.configurations.compile in project.idea.module.scopes.COMPILE.plus
        project.configurations.runtime in project.idea.module.scopes.RUNTIME.plus
        project.configurations.compile in project.idea.module.scopes.RUNTIME.minus
        project.configurations.test in project.idea.module.scopes.TEST.plus
        project.configurations.runtime in project.idea.module.scopes.TEST.minus
    }

    def "idea modules has project source and test source configured"() {
        expect:
        [
                'src/groovy', 'src/java', 'grails-app/controllers',
                'grails-app/domain', 'grails-app/services', 'grails-app/taglib'
        ].each {
            def directory = project.file(it)
            assert project.idea.module.sourceDirs.contains(directory)
        }

        and:
        [
                'test/unit', 'test/integration', 'test/functional'
        ].each {
            def directory = project.file(it)
            assert project.idea.module.testSourceDirs.contains(directory)
        }
    }

    def "idea module has plugin source directories configured"() {
        given:
        project.dependencies {
            bootstrap 'org.grails.plugins:tomcat:7.0.50.1'
            compile 'org.grails.plugins:resources:1.2.2'
            runtime 'org.grails.plugins:zipped-resources:1.0.1'
            test 'org.grails.plugins:build-test-data:2.1.1'
        }
        def srcDirs = [
                'src/groovy', 'src/java', 'grails-app/controllers',
                'grails-app/domain', 'grails-app/services', 'grails-app/taglib'
        ]

        expect:
        ['tomcat-7.0.50.1', 'resources-1.2.2', 'zipped-resources-1.0.1'].each { plugin ->
            srcDirs.each { subdir ->
                def directory = project.file("buildPlugins/$plugin/$subdir")
                assert project.idea.module.sourceDirs.contains(directory)
            }
        }

        and:
        assert !project.idea.module.sourceDirs.any {
            it.path.startsWith(project.file('buildPlugins/build-test-data-2.1.1').path)
        }

        and:
        srcDirs.each { subdir ->
            def directory = project.file("buildPlugins/build-test-data-2.1.1/$subdir")
            assert project.idea.module.testSourceDirs.contains(directory)
        }
    }

}
