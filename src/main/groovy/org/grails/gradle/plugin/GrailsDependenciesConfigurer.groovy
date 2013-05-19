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
import org.gradle.api.artifacts.ModuleDependency
import org.grails.launcher.version.GrailsVersion
import org.grails.launcher.version.GrailsVersionQuirks

class GrailsDependenciesConfigurer {

    private final Project project
    private final GrailsVersion grailsVersion
    private final GrailsVersionQuirks grailsVersionQuirks

    GrailsDependenciesConfigurer(Project project, String grailsVersion) {
        this.project = project
        this.grailsVersion = GrailsVersion.parse(grailsVersion)
        this.grailsVersionQuirks = new GrailsVersionQuirks(grailsVersion)
    }

    void configureBootstrapClasspath(Configuration configuration) {
        ["bootstrap", "scripts", "resources"].each {
            addDependency("org.grails:grails-$it:${grailsVersion}", configuration).exclude(group: "org.slf4j")
        }

        if (grailsVersion.is(1)) {
            addDependency("org.apache.ant:ant:1.8.2", configuration)
        }

        if (grailsVersionQuirks.isRequiresExplicitIvyDependency()) {
            addDependency("org.apache.ivy:ivy:2.1.0", configuration)
        }

        if (grailsVersionQuirks.isRequiresExplicitLoggingBootstrapDependencies()) {
            addDependency("org.slf4j:jcl-over-slf4j:1.6.2", configuration);
            addDependency("log4j:log4j:1.2.17", configuration);
        }
    }

    void configureCompileClasspath(Configuration configuration) {
        if (grailsVersionQuirks.isHasGrailsDependenciesPom()) {
            addDependency("org.grails:grails-dependencies:${grailsVersion}", configuration)
        }
    }

    void configureResources(Configuration configuration) {
        addDependency("org.grails:grails-resources:$grailsVersion", configuration).transitive = false
    }

    private ModuleDependency addDependency(String notation, Configuration configuration) {
        ModuleDependency dependency = project.dependencies.create(notation) as ModuleDependency
        configuration.dependencies.add(dependency)
        dependency
    }

}
