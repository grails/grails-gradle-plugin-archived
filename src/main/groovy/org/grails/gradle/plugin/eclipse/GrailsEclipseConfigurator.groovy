/*
 * Copyright 2014 the original author or authors.
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

package org.grails.gradle.plugin.eclipse

import org.gradle.api.Project
import org.gradle.api.tasks.Delete
import org.gradle.plugins.ide.eclipse.EclipsePlugin
import org.gradle.plugins.ide.eclipse.model.EclipseModel
import org.grails.gradle.plugin.tasks.GrailsEclipseJdtGroovyTask

/**
 * Configure the Eclipse IDE integration for the project.
 *
 * Created by Jeevanandam M. (jeeva@myjeeva.com) on 7/4/14.
 */
class GrailsEclipseConfigurator {
    static final GRADLE_GRAILS_PLUGIN_DIR_LINK_NAME = '.link_to_grails_plugins'
    static final GRADLE_GRAILS_PLUGIN_RELATIVE_DIR = 'buildPlugins'
    static final GRADLE_GRAILS_OUTPUT_RELATIVE_DIR = 'build/classes'

    /**
     * Registering Eclipse IDE project configuration
     *
     * @param project gradle project instance of {@code Project}
     */
    void configure(Project project) {
        project.plugins.withType(EclipsePlugin) {
            project.eclipse {
                createEclipseProject(project, model)
                createEclipseClasspath(project, model)
            }

            configureTasksAndHooks(project)
        }
    }

    private void createEclipseProject(Project project, EclipseModel model) {
        model.project {
            buildCommand 'org.eclipse.wst.common.project.facet.core.builder'
            buildCommand 'org.eclipse.jdt.core.javabuilder'

            natures = ['org.grails.ide.eclipse.core.nature',
                       'org.eclipse.jdt.groovy.core.groovyNature',
                       'org.eclipse.jdt.core.javanature',
                       'org.eclipse.wst.common.project.facet.core.nature']

            linkedResource name: GRADLE_GRAILS_PLUGIN_DIR_LINK_NAME,
                type: '2',
                location: "${project.projectDir.absolutePath}${File.separator}${GRADLE_GRAILS_PLUGIN_RELATIVE_DIR}"

            file.withXml {
                def node = it.asNode()

                /*
                 *  Project filters for build, .gradle, target
                 */
                Node filteredResources = new XmlParser().parseText("""<filteredResources>
                        <filter>
                            <id>1407523962826</id>
                            <name/>
                            <type>10</type>
                            <matcher>
                                <id>org.eclipse.ui.ide.multiFilter</id>
                                <arguments>1.0-name-matches-false-false-target</arguments>
                            </matcher>
                        </filter>
                        <filter>
                            <id>1407523962836</id>
                            <name/>
                            <type>10</type>
                            <matcher>
                                <id>org.eclipse.ui.ide.multiFilter</id>
                                <arguments>1.0-name-matches-false-false-build</arguments>
                            </matcher>
                        </filter>
                        <filter>
                            <id>1407529877923</id>
                            <name/>
                            <type>10</type>
                            <matcher>
                                <id>org.eclipse.ui.ide.multiFilter</id>
                                <arguments>1.0-name-matches-false-false-.gradle</arguments>
                            </matcher>
                        </filter>
                    </filteredResources>""")

                node.append(filteredResources)
            }
        }
    }

    private void createEclipseClasspath(Project project, EclipseModel model) {
        model.classpath {
            defaultOutputDir = new File(GRADLE_GRAILS_OUTPUT_RELATIVE_DIR)

            containers.clear()

            def configurations = project.configurations
            plusConfigurations += [configurations.bootstrap]
            plusConfigurations += [configurations.runtime]
            plusConfigurations += [configurations.test]

            file.withXml {
                def node = it.asNode()

                // Excluding resources source directories
                (project.sourceSets.main.resources.srcDirs as LinkedHashSet).each {
                    def path = project.relativePath(it)
                    node.remove(node.'**'.find {
                        it.@path == path
                    })
                }

                // Containers
                node.appendNode 'classpathentry', [kind: 'con', path: 'org.eclipse.jdt.launching.JRE_CONTAINER']
                node.appendNode 'classpathentry', [kind: 'con', path: 'GROOVY_DSL_SUPPORT']
            }
        }
    }

    private void configureTasksAndHooks(Project project) {
        // cleanEclipseJdtGroovy task
        project.tasks.create(GrailsEclipseJdtGroovyTask.ECLIPSE_JDT_GROOVY_CLEAN_TASK_NAME, Delete.class).with {
            description = 'Cleans the Eclipse JDT Groovy settings file.'
            delete GrailsEclipseJdtGroovyTask.ECLIPSE_GROOVY_JDT_PREFS_FILE
        }
        project.tasks.findByName('cleanEclipse')?.dependsOn(GrailsEclipseJdtGroovyTask.ECLIPSE_JDT_GROOVY_CLEAN_TASK_NAME)

        // eclipseJdtGroovy task
        project.task(GrailsEclipseJdtGroovyTask.ECLIPSE_JDT_GROOVY_TASK_NAME, type: GrailsEclipseJdtGroovyTask)
    }
}
