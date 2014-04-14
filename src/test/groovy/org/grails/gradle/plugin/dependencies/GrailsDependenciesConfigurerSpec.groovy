package org.grails.gradle.plugin.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.internal.reflect.Instantiator
import org.gradle.testfixtures.ProjectBuilder
import org.grails.gradle.plugin.GrailsProject
import org.grails.gradle.plugin.internal.DefaultGrailsProject
import org.grails.launcher.version.GrailsVersion
import spock.lang.Specification
import spock.lang.Unroll

class GrailsDependenciesConfigurerSpec extends Specification {

    Project project
    GrailsProject grailsProject
    GrailsVersion version
    GrailsDependenciesConfigurer dependenciesConfigurer

    def setup() {
        project = ProjectBuilder.builder().withName('dependency-spec').build()
        grailsProject = new DefaultGrailsProject(project, project.services.get(Instantiator))
        version = GrailsVersion.parse('2.3.5')
        dependenciesConfigurer = new GrailsDependenciesConfigurer(project, grailsProject, version)
    }

    @Unroll
    def 'configure springloaded #springLoadedVersion as #dependencyTarget'() {
        given:
        Configuration configuration = project.configurations.create('springloaded')

        when:
        grailsProject.springLoadedVersion = springLoadedVersion
        dependenciesConfigurer.configureSpringloaded(configuration)

        then:
        configuration.dependencies.size() == 1
        def dependency = configuration.dependencies.asList().first()
        assert "${dependency.group}:${dependency.name}:${dependency.version}" == dependencyTarget

        where:
        springLoadedVersion || dependencyTarget
        '1.1.4' || 'org.springsource.springloaded:springloaded-core:1.1.4'
        '1.1.5.RELEASE' || 'org.springframework:springloaded:1.1.5.RELEASE'
    }
}
