package org.grails.gradle.plugin.dependencies

import org.gradle.api.Project
import org.grails.gradle.plugin.GrailsProject
import org.grails.launcher.version.GrailsVersion

class DependencyConfigurerFactory {

    static DependencyConfigurer build(Project project, GrailsProject grailsProject) {
        GrailsVersion version = GrailsVersion.parse(grailsProject.grailsVersion)
        return new GrailsDependenciesConfigurer(project, grailsProject, version)
    }
}
