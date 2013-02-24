/*
 * Copyright 2013 the original author or authors.
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

package org.grails.gradle.plugin.internal

import org.gradle.api.logging.Logger
import org.gradle.process.JavaExecSpec
import org.grails.launcher.context.GrailsLaunchContext
import org.grails.launcher.context.SerializableGrailsLaunchContext
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

class GrailsLaunchConfigureActionSpec extends Specification {
    @Rule final TemporaryFolder dir = new TemporaryFolder()

    File grailsHome
    File contextDestination
    GrailsLaunchContext launchContext = new SerializableGrailsLaunchContext()
    Logger logger = Mock()
    JavaExecSpec javaExecSpec = Mock()
    GrailsLaunchConfigureAction launchConfigureAction

    def setup() {
        grailsHome = dir.newFolder()
        contextDestination = dir.newFile()
        launchConfigureAction = new GrailsLaunchConfigureAction(launchContext, contextDestination, logger)
    }

    def "grails home is passed as a system property to launcher when set"() {
        given:
        launchContext.buildDependencies = []

        when: "the action is executed without grailsHome set"
        launchConfigureAction.execute(javaExecSpec)

        then:
        0 * javaExecSpec.systemProperty("grails.home", _)

        when: "the action is executed with grailsHome set"
        launchContext.grailsHome = grailsHome
        launchConfigureAction.execute(javaExecSpec)

        then:
        1 * javaExecSpec.systemProperty("grails.home", grailsHome.absolutePath)
    }
}
