package org.grails.gradle.plugin.dependencies

import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.Dependency
import org.gradle.internal.reflect.Instantiator
import org.gradle.testfixtures.ProjectBuilder
import org.grails.gradle.plugin.GrailsPlugin
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

    def "configure base dependencies"() {
        given:
        project.plugins.apply GrailsPlugin
        GrailsProject grails = project.grails
        grails.grailsVersion = applyVersion

        when:
        println 'hi'

        then:
        interaction {
            bootstrap.each { hasDependency(project.configurations.bootstrap, it) }
            provided.each { hasDependency(project.configurations.provided, it) }
            compile.each { hasDependency(project.configurations.compile, it) }
            runtime.each { hasDependency(project.configurations.runtime, it) }
            test.each { hasDependency(project.configurations.test, it) }
            resources.each { hasDependency(project.configurations.resources, it) }
        }

        where:
        applyVersion << ['2.3.7', '2.4.0']
        bootstrap << [
                [
                        'org.grails:grails-bootstrap:2.3.7',
                        'org.grails:grails-scripts:2.3.7',
                        'org.grails:grails-resources:2.3.7',

                ],
                [
                        'org.grails:grails-bootstrap:2.4.0',
                        'org.grails:grails-scripts:2.4.0',
                        'org.grails:grails-resources:2.4.0'
                ]
        ]
        provided << [
                [], []
        ]
        compile << [
                [
                        'org.grails:grails-dependencies:2.3.7'
                ],
                [
                        'org.grails:grails-dependencies:2.4.0',
                ]
        ]
        runtime << [
                [
                        'com.h2database:h2:1.3.170'
                ],
                [
                        'com.h2database:h2:1.3.170'
                ]
        ]
        test << [
                [
                        'org.grails:grails-plugin-testing:2.3.7',
                        'org.grails:grails-test:2.3.7'
                ],
                [
                        'org.grails:grails-plugin-testing:2.4.0',
                        'org.grails:grails-test:2.4.0',
                        'junit:junit:4.11',
                        'org.spockframework:spock-core:0.7-groovy-2.0'
                ]
        ]
        resources << [
                [
                        'org.grails:grails-resources:2.3.7'
                ],
                [
                        'org.grails:grails-resources:2.4.0'
                ]
        ]
    }

    private void hasDependency(Configuration configuration, String dependency) {
        def tokens = dependency.split(':')
        def group = tokens[0]
        def name = tokens[1]
        def version = tokens[2]
        def artifact = configuration.dependencies.find {
            it.group == group && it.name == name && it.version == version
        }
        assert artifact, "Could not find artifact $group:$name:$version"
    }

}
