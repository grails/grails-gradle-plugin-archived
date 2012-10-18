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

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration

class GrailsDependenciesUtil {

    static configureBootstrapClasspath(Project project, String grailsVersion, Configuration configuration) {
        String name = configuration.name

        project.dependencies {
            if (grailsVersion.startsWith("1.")) {
                ["bootstrap", "scripts", "resources"].each {
                    "$name"("org.grails:grails-$it:${grailsVersion}") {
                        exclude group: "org.slf4j"
                    }
                }

                if (grailsVersion.startsWith("1.3")) {
                    "$name"("org.apache.ant:ant:1.8.2")
                }

                if (!grailsVersion.startsWith("2")) {
                    "$name"("org.apache.ivy:ivy:2.1.0")
                }
            } else {
                "$name"("org.grails:grails-dependencies:${grailsVersion}")
                "$name"("org.grails:grails-scripts:${grailsVersion}")
                "$name"("org.grails:grails-resources:${grailsVersion}")
            }
        }
    }

}
