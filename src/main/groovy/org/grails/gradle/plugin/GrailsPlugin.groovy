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
import org.gradle.api.internal.ConventionMapping
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.plugins.BasePlugin
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.base.plugins.LanguageBasePlugin
import org.grails.gradle.plugin.idea.GrailsIdeaConfigurator
import org.grails.gradle.plugin.internal.DefaultGrailsProject
import org.grails.gradle.plugin.tasks.GrailsTask
import org.grails.gradle.plugin.tasks.GrailsTaskConfigurator

import javax.inject.Inject

class GrailsPlugin implements Plugin<Project> {

    GrailsTaskConfigurator taskConfigurator
    GrailsSourceSetConfigurator sourceSetConfigurator
    GrailsIdeaConfigurator ideaConfigurator

    private final Instantiator instantiator;
    private final FileResolver fileResolver

    @Inject
    GrailsPlugin(Instantiator instantiator, FileResolver fileResolver) {
        this.instantiator = instantiator;
        this.fileResolver = fileResolver;
        this.sourceSetConfigurator = new GrailsSourceSetConfigurator(instantiator, fileResolver)
        this.taskConfigurator = new GrailsTaskConfigurator()
        this.ideaConfigurator = new GrailsIdeaConfigurator()
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
            def dependenciesUtil = new GrailsDependenciesConfigurer(project, grailsProject.grailsVersion)
            dependenciesUtil.configureBootstrapClasspath(bootstrapConfiguration)
            dependenciesUtil.configureCompileClasspath(compileConfiguration)
            dependenciesUtil.configureTestClasspath(testConfiguration)
            dependenciesUtil.configureResources(resourcesConfiguration)
        }

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

        configureSourceSets(project, grailsProject)
        configureTasks(project, grailsProject)
        configureIdea(project)
    }

    void configureTasks(Project project, GrailsProject grailsProject) {
        taskConfigurator.configure(project, grailsProject)
    }

    void configureSourceSets(Project project, GrailsProject grailsProject) {
        sourceSetConfigurator.configure(project.extensions.getByType(ProjectSourceSet), grailsProject)
    }

    void configureIdea(Project project) {
        ideaConfigurator.configure(project)
    }

    Configuration getOrCreateConfiguration(Project project, String name) {
        ConfigurationContainer container = project.configurations
        container.findByName(name) ?: container.create(name)
    }
}
