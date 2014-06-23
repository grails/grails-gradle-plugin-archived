package org.grails.gradle.plugin.idea

import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.api.artifacts.ProjectDependency
import org.gradle.plugins.ide.idea.IdeaPlugin
import org.gradle.plugins.ide.idea.model.IdeaModule

/**
 * Configure the IDEA integration for the project.
 */
class GrailsIdeaConfigurator {

    void configure(Project project) {
        project.plugins.withType(IdeaPlugin) {
            project.idea {
                def configurations = project.configurations
                //Map IDEA scopes to Gradle configurations
                module.scopes = [
                        PROVIDED: [plus: [configurations.provided], minus: []],
                        COMPILE: [plus: [configurations.compile], minus: []],
                        RUNTIME: [plus: [configurations.runtime], minus: [configurations.compile]],
                        TEST: [plus: [configurations.test], minus: [configurations.runtime]]
                ]
                //Configure additional source and test directories
                sourceDirs(project, module)
                testDirs(project, module)
                pluginSourceDirs(project, module)
                module.conventionMapping.excludeDirs = { excludedBuildFiles(project) }
                module.iml.withXml { XmlProvider xmlProvider ->
                    declareGrailsFacets(project, xmlProvider.asNode())
                }
            }
        }
    }

    private LinkedHashSet excludedBuildFiles(Project project) {
        return [project.buildDir, project.file('.gradle')] as LinkedHashSet
    }

    /**
     * Register the source directories for the project and any plugins with IDEA
     */
    private void sourceDirs(Project project, IdeaModule module) {
        module.conventionMapping.sourceDirs = {
            (project.sourceSets.main.allSource.srcDirs as LinkedHashSet) -
                    (project.sourceSets.main.resources.srcDirs as LinkedHashSet)
        }
    }

    /**
     * Calculate that plugin source directories. This is required because Gradle treats the plugin zip file
     * as a jar, assuming that all the directories contained within are packages.
     *
     * This is a little hacky in that it assumes that all plugins have the 'org.grails.plugins' group.
     * This needs to be made more robust (perhaps a 'pluginCompile' and 'pluginRuntime' configuration)
     */
    private void pluginSourceDirs(Project project, IdeaModule module) {
        ['bootstrap', 'compile', 'runtime'].each { name ->
            registerPluginSourceDirectory(project, project.configurations.getByName(name)) { dir ->
                module.sourceDirs += dir
            }
        }
        registerPluginSourceDirectory(project, project.configurations.test) { dir ->
            module.testSourceDirs += dir
        }
    }

    private void registerPluginSourceDirectory(Project project, Configuration configuration, Closure mapping) {
        configuration.allDependencies.matching({ dependency ->
            isPluginZip(dependency)
        }).all { dependency ->
            ['src/groovy', 'grails-app/i18n', 'grails-app/controllers', 'grails-app/domain',
                    'grails-app/services', 'grails-app/taglib', 'src/java'].collect { root ->
                mapping(pluginDir(dependency, project, root))
            }
        }
    }

    private boolean isPluginZip(Dependency dependency) {
        if (!(dependency instanceof ProjectDependency) &&
                dependency.group == 'org.grails.plugins') {
            return true
        }
        return false
    }

    private void testDirs(Project project, IdeaModule module) {
        module.conventionMapping.testSourceDirs = {
            project.sourceSets.test.allSource.srcDirs as LinkedHashSet
        }
    }

    private File pluginDir(Dependency dependency, Project project, String root) {
        return new File(project.projectDir, '/buildPlugins/' + dependency.name + '-' + dependency.version + '/' + root)
    }

    /**
     * Tries to mimic the IML creation that IDEA natively does for Grails projects.
     * @param project
     * @param iml
     */
    private void declareGrailsFacets(Project project, Node iml) {
        Node facetManager = iml.component.find { it.@name == "FacetManager" }
        if (!facetManager) {
            facetManager = iml.appendNode("component", [name: "FacetManager"])
        }
        declareSpringFacet(facetManager)
        declareGrailsWebFacet(facetManager)
        declareHibernateFacet(facetManager)
    }

    private void declareSpringFacet(Node facetManager) {
        Node spring = facetManager.facet.find { it.@type == 'Spring' }
        if (!spring) {
            spring = facetManager.appendNode('Spring', [type: 'Spring', name: 'Spring'])
        }
        Node configuration = spring.configuration[0]
        if (!configuration) {
            configuration = spring.appendNode('configuration')
        }
        Node fileset = configuration.fileset.find { it.@id == 'Grails' }
        if (!fileset) {
            fileset = configuration.appendNode('fileset', [id: 'Grails', name: 'Grails', removed: 'false'])
        }
        ['web-app/WEB-INF/applicationContext.xml', 'grails-app/conf/spring/resources.xml'].each { file ->
            Node fileNode = fileset.file.find { it.text().endsWith(file) }
            if (!fileNode) {
                fileset.appendNode('file', "file://\$MODULE_DIR\$/${file}")
            }
        }
    }

    private void declareGrailsWebFacet(Node facetManager) {
        Node grailsWeb = facetManager.facet.find { it.@type == "web" }
        if (!grailsWeb) {
            grailsWeb = facetManager.appendNode("facet", [type: "web", name: "GrailsWeb"])
        }
        Node configuration = grailsWeb.configuration[0]
        if (!configuration) {
            configuration = grailsWeb.appendNode("configuration")
        }
        Node webroots = configuration.webroots[0]
        if (!webroots) {
            webroots = configuration.appendNode('webroots')
        }
        ['grails-app/views', 'web-app'].each { root ->
            Node rootNode = webroots.root.find { it.@url.endsWith(root) }
            if (!rootNode) {
                webroots.appendNode('root', [url: "file://\$MODULE_DIR\$/${root}", relative: '/'])
            }
        }
        Node sourceRoots = configuration.sourceRoots[0]
        if (!sourceRoots) {
            configuration.appendNode('sourceRoots')
        }
    }

    private void declareHibernateFacet(Node facetManager) {
        Node hibernate = facetManager.facet.find { it.@type == 'hibernate' }
        if (!hibernate) {
            hibernate = facetManager.appendNode('hibernate', [type: 'hibernate', name: 'Hibernate'])
        }
        Node configuration = hibernate.configuration[0]
        if (!configuration) {
            configuration = hibernate.appendNode('configuration')
        }
        Node datasourceMap = configuration.get('datasource-map')[0]
        if (!datasourceMap) {
            configuration.appendNode('datasource-map')
        }
    }
}
