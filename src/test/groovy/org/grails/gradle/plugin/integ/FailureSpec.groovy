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

package org.grails.gradle.plugin.integ

class FailureSpec extends IntegSpec {

    def "handles failure"() {
        given:
        applyPlugin()
        buildFile << """
            grails.grailsVersion '2.0.1'
        """

        buildFile << """
            init {
            // jvmOptions { jvmArgs "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5005" }
            }

            task "package"(type: GrailsTask) {
                //jvmOptions { jvmArgs "-Xdebug", "-Xrunjdwp:transport=dt_socket,server=y,suspend=y,address=5008" }
            }
        """

        when:
        launcher("init", "-s").run().rethrowFailure()
        def buildConfig = file("grails-app/conf/BuildConfig.groovy")
        buildConfig.text = buildConfig.text.replace("dependencies {", "dependencies {\ncompile('org.spockframework:spock-grails-support:0.7-groovy-1.8')")
        println buildConfig.text
        launcher("package", "-s").run().rethrowFailure()

        then:
        task("package").state.didWork

        and:
        file("grails-app").exists()
    }
}
