package org.grails.gradle.plugin.dependencies

import org.gradle.api.Project
import org.grails.gradle.plugin.GrailsProject
import org.grails.launcher.version.GrailsVersion

/**
 * Creates the proper dependency configuration instance for this Grails version.
 * Currently all project use the same configuration, but future version may require
 * more substantial customization.
 */
class DependencyConfigurerFactory {

    static DependencyConfigurer build(Project project, GrailsProject grailsProject) {
        GrailsVersion version = GrailsVersion.parse(grailsProject.grailsVersion)
        return new GrailsDependenciesConfigurer(project, grailsProject, version)
    }
}
