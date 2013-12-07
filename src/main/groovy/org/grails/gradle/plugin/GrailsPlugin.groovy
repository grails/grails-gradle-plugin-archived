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

import org.gradle.api.InvalidUserDataException
import org.gradle.api.Plugin
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ConfigurationContainer
import org.gradle.api.artifacts.DependencyResolveDetails
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.BasePlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.plugins.LanguageBasePlugin
import org.grails.gradle.plugin.dependencies.DependencyConfigurer
import org.grails.gradle.plugin.dependencies.DependencyConfigurerFactory
import org.grails.gradle.plugin.idea.GrailsIdeaConfigurator
import org.grails.gradle.plugin.internal.DefaultGrailsProject
import org.grails.gradle.plugin.tasks.GrailsTask
import org.grails.gradle.plugin.tasks.GrailsTaskConfigurator

import javax.inject.Inject

/**
 * Configures a Gradle project as a Grails project.
 */
class GrailsPlugin implements Plugin<Project> {

    private final Instantiator instantiator
    private final FileResolver fileResolver

    @Inject
    GrailsPlugin(Instantiator instantiator, FileResolver fileResolver) {
        this.instantiator = instantiator
        this.fileResolver = fileResolver
    }

    void apply(Project project) {
        project.plugins.apply(BasePlugin)
        project.plugins.apply(LanguageBasePlugin)

        DefaultGrailsProject grailsProject = project.extensions.create('grails', DefaultGrailsProject, project, instantiator)
        project.convention.plugins.put('grails', grailsProject)

        grailsProject.conventionMapping.with {
            map("projectDir") { project.projectDir }
            map("projectWorkDir") { project.buildDir }
        }

        Configuration bootstrapConfiguration = getOrCreateConfiguration(project, "bootstrap")

        Configuration compileConfiguration = getOrCreateConfiguration(project, "compile")
        Configuration providedConfiguration = getOrCreateConfiguration(project, "provided")
        Configuration runtimeConfiguration = getOrCreateConfiguration(project, "runtime")
        Configuration testConfiguration = getOrCreateConfiguration(project, "test")
        Configuration resourcesConfiguration = getOrCreateConfiguration(project, "resources")
        Configuration springloadedConfiguration = getOrCreateConfiguration(project, "springloaded")

        runtimeConfiguration.extendsFrom(compileConfiguration)
        testConfiguration.extendsFrom(runtimeConfiguration)

        grailsProject.onSetGrailsVersion { String grailsVersion ->
            DependencyConfigurer dependenciesUtil = DependencyConfigurerFactory.build(project, grailsProject)
            dependenciesUtil.configureBootstrapClasspath(bootstrapConfiguration)
            dependenciesUtil.configureProvidedClasspath(providedConfiguration)
            dependenciesUtil.configureCompileClasspath(compileConfiguration)
            dependenciesUtil.configureRuntimeClasspath(runtimeConfiguration)
            dependenciesUtil.configureTestClasspath(testConfiguration)
            dependenciesUtil.configureResources(resourcesConfiguration)
        }
        grailsProject.onSetGroovyVersion { String groovyVersion ->
            DependencyConfigurer dependenciesUtil = DependencyConfigurerFactory.build(project, grailsProject)
            dependenciesUtil.configureGroovyBootstrapClasspath(bootstrapConfiguration)
            dependenciesUtil.configureGroovyCompileClasspath(compileConfiguration)

            project.configurations.all { Configuration config ->
                config.resolutionStrategy {
                    eachDependency { DependencyResolveDetails details ->
                        if (details.requested.group == 'org.codehaus.groovy') {
                            if (details.requested.name == 'groovy-all') {
                                details.useVersion groovyVersion
                            }
                            if (details.requested.name == 'groovy') {
                                details.useTarget name: 'groovy-all', version: groovyVersion
                            }
                        }
                    }
                }
            }
        }

        configureSourceSets(project, grailsProject)
        configureTasks(project, grailsProject)
        project.tasks.withType(GrailsTask) { GrailsTask task ->
            ConventionMapping conventionMapping = task.conventionMapping
            conventionMapping.with {
                map("projectDir") { grailsProject.projectDir }
                map("projectWorkDir") { grailsProject.projectWorkDir }
                map("grailsVersion") { grailsProject.grailsVersion }

                map("bootstrapClasspath") { bootstrapConfiguration }

                map("providedClasspath") { providedConfiguration }
                map("compileClasspath") { compileConfiguration }
                map("runtimeClasspath") { runtimeConfiguration }
                map("testClasspath") { testConfiguration }
                map("sourceSets") { grailsProject.sourceSets }

                map("springloaded") {
                    if (springloadedConfiguration.dependencies.empty) {
                        def defaultSpringloaded = project.dependencies.create("org.springsource.springloaded:springloaded-core:$grailsProject.springLoadedVersion")
                        springloadedConfiguration.dependencies.add(defaultSpringloaded)
                    }

                    def lenient = springloadedConfiguration.resolvedConfiguration.lenientConfiguration
                    if (lenient.unresolvedModuleDependencies) {
                        def springloadedDependency = springloadedConfiguration.dependencies.toList().first()
                        project.logger.warn("Failed to resolve springloaded dependency: $springloadedDependency (reloading will be disabled)")
                        null
                    } else {
                        springloadedConfiguration
                    }
                }
            }

            doFirst {
                if (grailsProject.grailsVersion == null) {
                    throw new InvalidUserDataException("You must set 'grails.grailsVersion' property before Grails tasks can be run")
                }
            }
        }
        configureIdea(project)
    }

    void configureTasks(Project project, GrailsProject grailsProject) {
        new GrailsTaskConfigurator().configure(project, grailsProject)
    }

    void configureSourceSets(Project project, GrailsProject grailsProject) {
        new GrailsSourceSetConfigurator(instantiator, fileResolver).configure(project.extensions.getByType(ProjectSourceSet), grailsProject)
    }

    void configureIdea(Project project) {
        new GrailsIdeaConfigurator().configure(project)
    }

    Configuration getOrCreateConfiguration(Project project, String name) {
        ConfigurationContainer container = project.configurations
        container.findByName(name) ?: container.create(name)
    }
}
