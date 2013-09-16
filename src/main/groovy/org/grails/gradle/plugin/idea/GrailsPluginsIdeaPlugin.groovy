package org.grails.gradle.plugin.idea

import org.gradle.api.Project
import org.gradle.api.artifacts.Dependency
import org.gradle.internal.reflect.Instantiator
import org.gradle.plugins.ide.idea.GenerateIdeaModule
import org.gradle.plugins.ide.idea.model.IdeaModule
import org.gradle.plugins.ide.idea.model.IdeaModuleIml
import org.gradle.plugins.ide.idea.model.PathFactory
import org.gradle.plugins.ide.internal.IdePlugin

import javax.inject.Inject

class GrailsPluginsIdeaPlugin extends IdePlugin {

    Instantiator instantiator
    IdeaModule pluginModule

    @Inject
    GrailsPluginsIdeaPlugin(Instantiator instantiator) {
        this.instantiator = instantiator
    }

    void onApply(Project project) {
        def task = project.task('grailsPluginsIdeaModule', description: 'Generates IDEA module files (IML) for Grails plugins', type: GenerateIdeaModule) {
            def iml = new IdeaModuleIml(xmlTransformer, project.projectDir)
            module = instantiator.newInstance(IdeaModule, project, iml)
            pluginModule = module

            module.conventionMapping.sourceDirs = { [] as LinkedHashSet }
            module.conventionMapping.name = { project.name + '-grailsPlugins' }
            module.conventionMapping.contentRoot = { project.buildDir + '/plugins/' }
            module.conventionMapping.testSourceDirs = { [] as LinkedHashSet }
            module.conventionMapping.excludeDirs = { [project.buildDir, project.file('.gradle')] as LinkedHashSet }

            module.conventionMapping.pathFactory = {
                PathFactory factory = new PathFactory()
                factory.addPathVariable('MODULE_DIR', outputFile.parentFile)
                module.pathVariables.each { key, value ->
                    factory.addPathVariable(key, value)
                }
                factory
            }
        }
        project.tasks.ideaModule.dependsOn(task)
        configure(project)
        addWorker(task)
    }

    void configure(Project project) {
        def pluginSources = []
        ['compile', 'test', 'runtime'].each { scope ->
            project.configurations.getByName(scope).dependencies.each { dependency ->
                if (dependency.group == 'org.grails.plugins') {
                    ['src/groovy', 'grails-app/i18n', 'grails-app/controllers', 'grails-app/domain', 'grails-app/services', 'grails-app/taglib', 'src/java'].each { root ->
                        pluginSources << pluginDir(dependency, project) + root
                    }
                }
            }
        }
        pluginModule.conventionMapping.sourceDirs = pluginSources as LinkedHashSet
    }

    String pluginDir(Dependency dependency, Project project) {
        return project.buildDir + '/plugins/' + dependency.name + '-' + dependency.version + '/'
    }

    @Override protected String getLifecycleTaskName() {
        return 'idea'
    }
}
