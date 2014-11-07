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
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.api.plugins.BasePlugin
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
    static final GRADLE_GRAILS_OUTPUT_RELATIVE_DIR = 'target-eclipse/classes'
    static final ECLIPSE_CLASS_PATH_ENTRY_NODE_NAME = 'classpathentry'

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
        String pluginLinkLocation = "${project.projectDir.absolutePath}${File.separator}${GRADLE_GRAILS_PLUGIN_RELATIVE_DIR}"

        model.project {
            buildCommands.clear()
            buildCommand 'org.eclipse.wst.common.project.facet.core.builder'
            buildCommand 'org.eclipse.jdt.core.javabuilder'

            natures = ['org.grails.ide.eclipse.core.nature',
                       'org.eclipse.jdt.groovy.core.groovyNature',
                       'org.eclipse.jdt.core.javanature',
                       'org.eclipse.wst.common.project.facet.core.nature']

            linkedResource name: GRADLE_GRAILS_PLUGIN_DIR_LINK_NAME,
                type: '2',
                location: pluginLinkLocation

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
                        <filter>
                            <id>1407529877926</id>
                            <name/>
                            <type>10</type>
                            <matcher>
                                <id>org.eclipse.ui.ide.multiFilter</id>
                                <arguments>1.0-name-matches-false-false-buildPlugins</arguments>
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
                handleResourceSourceDirs(node)

                // Adding Plugin source directories
                handlePluginSourceDirs(project, node)

                // Containers
                node.appendNode ECLIPSE_CLASS_PATH_ENTRY_NODE_NAME, [kind: 'con', path: 'org.eclipse.jdt.launching.JRE_CONTAINER']
                node.appendNode ECLIPSE_CLASS_PATH_ENTRY_NODE_NAME, [kind: 'con', path: 'GROOVY_DSL_SUPPORT']
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

        // eclipseClasspath depends on assemble to resolve plugins
        project.tasks.findByName('eclipseClasspath')?.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME)
    }

    private void handleResourceSourceDirs(Node node) {
        ['grails-app/conf', 'grails-app/conf/hibernate', 'grails-app/conf/spring',
         'grails-app/views', 'web-app'].collect { dirPath ->
            def removeNode = node.'**'.find {
                it.@path == dirPath
            }

            if (removeNode) {
                node.remove(removeNode)
            }
        }
        node.appendNode(ECLIPSE_CLASS_PATH_ENTRY_NODE_NAME, [kind: 'src', path: 'grails-app/conf', excluding: 'spring/|hibernate/'])
    }

    private void handlePluginSourceDirs(Project project, Node node) {
        // Cleanup .zip from lib kind in the classpathentry
        node.'**'.findAll {
            it.@path?.toLowerCase()?.endsWith('.zip')
        }?.each {
            node.remove(it)
        }

        pluginSourceDirs(project).each { path ->
            def nodeValues = [kind: 'src', path: path]
            if (path.endsWith('grails-app/conf')) {
                nodeValues << [excluding: 'BuildConfig.groovy|*DataSource.groovy|UrlMappings.groovy|Config.groovy|BootStrap.groovy|spring/resources.groovy']
            }

            node.appendNode(ECLIPSE_CLASS_PATH_ENTRY_NODE_NAME, nodeValues)
                .appendNode('attributes')
                .appendNode('attribute', [name: 'org.grails.ide.eclipse.core.SOURCE_FOLDER', value: 'true'])
        }
    }

    /**
     * Note: Same hacky approach as GrailsIdeaConfigurator for plugin
     * directories processing. It assumes that all plugins have the 'org.grails.plugins' group.
     * */
    private pluginSourceDirs(Project project) {
        def plugins = [] as Set

        ['bootstrap', 'compile', 'runtime'].each { name ->
            Configuration configuration = project.configurations.getByName(name)
            if (configuration) {
                plugins.addAll(configuration.allDependencies.findAll {
                    !(it instanceof ProjectDependency) && it.group == 'org.grails.plugins'
                })
            }
        }

        def pluginPaths = []
        plugins.each { dependency ->
            ['src/groovy', 'grails-app/i18n', 'grails-app/controllers', 'grails-app/domain',
             'grails-app/services', 'grails-app/taglib', 'src/java', 'grails-app/conf'].each { relativePath ->
                String path = "${File.separator}${dependency.name}-${dependency.version}${File.separator}${relativePath}"
                File dir = new File(project.projectDir, "${File.separator}buildPlugins${path}")
                if (dir.exists()) {
                    pluginPaths << "${GRADLE_GRAILS_PLUGIN_DIR_LINK_NAME}${path}"
                }
            }
        }

        pluginPaths
    }
}
