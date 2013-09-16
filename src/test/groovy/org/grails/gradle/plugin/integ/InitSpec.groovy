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

import spock.lang.Unroll
import org.grails.launcher.version.GrailsVersion

@Unroll
class InitSpec extends IntegSpec {

    def "can create grails #grailsVersion project"() {
        given:
        buildFile << """
            grails.grailsVersion '$grailsVersion'
        """

        if (grailsVersion.is(2, 1)) {
            buildFile << """
                dependencies {
                    compile "org.codehaus.groovy:groovy-all:${grailsVersion.is(2,1,0) ? "1.8.6" : "1.8.8"}"
                    bootstrap "org.codehaus.groovy:groovy-all:${grailsVersion.is(2,1,0) ? "1.8.6" : "1.8.8"}"
                }
            """
        }

        if (grailsVersion.is(2, 2)) {
            buildFile << """
                dependencies {
                    compile "org.codehaus.groovy:groovy-all:2.0.5"
                    bootstrap "org.codehaus.groovy:groovy-all:2.0.5"
                    test "org.grails:grails-test:$grailsVersion"
                }
            """
        }

        when:
        launcher("init", "-s").run().rethrowFailure()

        then:
        task("init").state.didWork

        and:
        file("grails-app").exists()

        when:
        file("test/integration/SomeTest.groovy") << """
            class SomeTest extends GroovyTestCase {
                void testSomething() {
                    assert true
                }
            }
        """

        and:
        launcher("grails-test-app", "-s").run().rethrowFailure()

        then:
        task("grails-test-app").state.didWork

        where:
        grailsVersion << ["2.0.0", "2.1.0", "2.2.0.RC1"].collect { GrailsVersion.parse(it) }
    }

}
