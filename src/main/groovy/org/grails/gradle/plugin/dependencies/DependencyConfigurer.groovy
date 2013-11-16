package org.grails.gradle.plugin.dependencies

import groovy.transform.TupleConstructor
import org.gradle.api.Project
import org.gradle.api.artifacts.Configuration
import org.gradle.api.artifacts.ModuleDependency
import org.grails.gradle.plugin.GrailsProject
import org.grails.launcher.version.GrailsVersion
import org.grails.launcher.version.GrailsVersionQuirks

/**
 * Abstract class for the dependency configuration.
 */
@TupleConstructor
abstract class DependencyConfigurer {

    protected final Project project
    protected final GrailsProject grailsProject
    protected final GrailsVersion grailsVersion
    protected final GrailsVersionQuirks grailsVersionQuirks

    DependencyConfigurer(Project project, GrailsProject grailsProject, GrailsVersion grailsVersion) {
        this.project = project
        this.grailsProject = grailsProject
        this.grailsVersion = grailsVersion
        this.grailsVersionQuirks = new GrailsVersionQuirks(grailsVersion)
    }

    abstract void configureBootstrapClasspath(Configuration configuration)

    abstract void configureGroovyBootstrapClasspath(Configuration configuration)

    abstract void configureProvidedClasspath(Configuration configuration)

    abstract void configureCompileClasspath(Configuration configuration)

    abstract void configureGroovyCompileClasspath(Configuration configuration)

    abstract void configureRuntimeClasspath(Configuration configuration)

    abstract void configureTestClasspath(Configuration configuration)

    abstract void configureResources(Configuration configuration)

    protected ModuleDependency addDependency(String notation, Configuration configuration) {
        ModuleDependency dependency = project.dependencies.create(notation) as ModuleDependency
        configuration.dependencies.add(dependency)
        dependency
    }
}
