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

package org.grails.gradle.plugin.dependencies

import groovy.transform.InheritConstructors
import org.gradle.api.artifacts.Configuration

/**
 * Default Grails dependency configuration
 */
@InheritConstructors
class GrailsDependenciesConfigurer extends DependencyConfigurer {

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

    void configureGroovyBootstrapClasspath(Configuration configuration) {
        addGroovyDependency(configuration)
    }

    void configureProvidedClasspath(Configuration configuration) {

    }

    void configureCompileClasspath(Configuration configuration) {
        if (grailsVersionQuirks.isHasGrailsDependenciesPom()) {
            addDependency("org.grails:grails-dependencies:${grailsVersion}", configuration)
        }
    }

    void configureGroovyCompileClasspath(Configuration configuration) {
        addGroovyDependency(configuration)
    }

    void configureRuntimeClasspath(Configuration configuration) {
        if (grailsVersion.is(2) && grailsVersion.minor >= 3) {
            addDependency('com.h2database:h2:1.3.170', configuration)
        }
    }

    void configureTestClasspath(Configuration configuration) {
        addDependency("org.grails:grails-plugin-testing:${grailsVersion}", configuration)
        addDependency("org.grails:grails-test:${grailsVersion}", configuration)
    }

    void configureResources(Configuration configuration) {
        addDependency("org.grails:grails-resources:$grailsVersion", configuration).transitive = false
    }

    void configureSpringloaded(Configuration configuration) {
        String version = grailsProject.springLoadedVersion
        if (version >= '1.1.5' && !version.endsWith('.RELEASE')) {
            version += '.RELEASE'
        }
        if (grailsProject.springLoadedVersion.endsWith('.RELEASE')) {
            addDependency("org.springframework:springloaded:$version", configuration)
        } else {
            addDependency("org.springsource.springloaded:springloaded-core:$version", configuration)
        }
    }

    private addGroovyDependency(Configuration configuration) {
        addDependency("org.codehaus.groovy:groovy-all:${grailsProject.groovyVersion}", configuration)
    }

}
