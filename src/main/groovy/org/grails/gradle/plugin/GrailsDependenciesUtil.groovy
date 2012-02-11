package org.grails.gradle.plugin

import org.gradle.api.artifacts.Configuration
import org.gradle.api.Project

class GrailsDependenciesUtil {
    static configureBootstrapClasspath(Project project, String grailsVersion, Configuration configuration) {
        String name = configuration.name

        project.dependencies {
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
        }
    }
}
