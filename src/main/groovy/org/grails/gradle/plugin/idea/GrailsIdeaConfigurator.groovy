package org.grails.gradle.plugin.idea

import org.gradle.api.Project
import org.gradle.api.XmlProvider
import org.gradle.plugins.ide.idea.IdeaPlugin

class GrailsIdeaConfigurator {

    void configure(Project project) {
        project.plugins.withType(IdeaPlugin) {
            project.idea {
                def configurations = project.configurations
                module.scopes = [
                        PROVIDED: [plus: [configurations.provided], minus: []],
                        COMPILE: [plus: [configurations.compile], minus: []],
                        RUNTIME: [plus: [configurations.runtime], minus: [configurations.compile]],
                        TEST: [plus: [configurations.test], minus: [configurations.runtime]]
                ]
                module.conventionMapping.sourceDirs = { sourceDirs(project) }
                module.conventionMapping.testSourceDirs = { testDirs(project) }

                module.iml.withXml { XmlProvider xmlProvider ->
                    declareGrailsFacets(project, xmlProvider.asNode())
                    generateAndAppendPluginModule(project, xmlProvider.asNode())
                }
            }
        }
    }

    private LinkedHashSet sourceDirs(Project project) {
        (project.sourceSets.main.allSource.srcDirs as LinkedHashSet) -
                (project.sourceSets.main.resources.srcDirs as LinkedHashSet)
    }

    private LinkedHashSet testDirs(Project project) {
        project.sourceSets.test.allSource.srcDirs as LinkedHashSet
    }

    private void generateAndAppendPluginModule(Project project, Node iml) {

    }

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
