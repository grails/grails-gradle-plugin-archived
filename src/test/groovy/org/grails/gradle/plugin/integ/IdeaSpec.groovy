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

class IdeaSpec extends IntegSpec {

    def "dependencies carry over to idea module file"() {
        given:
        applyPlugin()
        buildFile << """
            apply plugin: 'idea'

            grails.grailsVersion '2.2.0'

            dependencies {
                compile 'org.codehaus.groovy:groovy-all:2.0.5'
                bootstrap 'org.codehaus.groovy:groovy-all:2.0.5'
                test 'org.grails:grails-test:2.2.0'
            }
        """

        when:
        launcher("idea", "-s").run().rethrowFailure()

        then:
        task("ideaModule").state.didWork

        and:
        String projectName = dir.root.name
        File moduleFile = new File(dir.root, "${projectName}.iml")
        moduleFile.exists()
        moduleFile.text.contains("groovy-all-2.0.5.jar")
        moduleFile.text.contains("grails-test-2.2.0.jar")
    }

}
