/*
 * Copyright 2012 the original author or authors.
 *
 * Licensed under the Apache License, Version 2.0 (the "License")
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

package org.grails.gradle.plugin.internal

import org.gradle.api.Action
import org.gradle.api.InvalidUserDataException
import org.gradle.api.NamedDomainObjectContainer
import org.gradle.api.Project
import org.gradle.api.artifacts.repositories.MavenArtifactRepository
import org.gradle.api.internal.project.ProjectInternal
import org.gradle.api.internal.tasks.DefaultSourceSetContainer
import org.gradle.api.tasks.SourceSet
import org.gradle.api.tasks.SourceSetContainer
import org.gradle.internal.reflect.Instantiator
import org.gradle.listener.ActionBroadcast
import org.grails.gradle.plugin.GrailsProject

/**
 * This is the 'grails' extension object for the Gradle DSL.
 */
public class DefaultGrailsProject implements GrailsProject {

    public static final String DEFAULT_SPRINGLOADED = "1.1.3"

    private final Project project

    private Object projectDir
    private Object projectWorkDir

    private String grailsVersion
    private String groovyVersion
    private String springLoadedVersion = DEFAULT_SPRINGLOADED

    private final SourceSetContainer sourceSets

    private ActionBroadcast<String> onSetGrailsVersion = new ActionBroadcast<String>()
    private ActionBroadcast<String> onSetGroovyVersion = new ActionBroadcast<String>()

    public DefaultGrailsProject(ProjectInternal project, Instantiator instantiator) {
        this.project = project
        this.sourceSets = instantiator.newInstance(DefaultSourceSetContainer.class,
                project.getFileResolver(), project.getTasks(), instantiator)
    }

    public File getProjectDir() {
        return projectDir == null ? null : project.file(projectDir)
    }

    public File getProjectWorkDir() {
        return projectWorkDir == null ? null : project.file(projectWorkDir)
    }

    public NamedDomainObjectContainer<SourceSet> sourceSets(Closure closure) {
        return sourceSets.configure(closure)
    }

    public SourceSetContainer getSourceSets() {
        return sourceSets
    }

    @Override
    //Set the Grails version and execute the configuration callback that configures the Grails dependencies
    //on the project
    public void setGrailsVersion(String grailsVersion) {
        if (this.grailsVersion != null) {
            throw new InvalidUserDataException("The 'grailsVersion' property can only be set once")
        }
        this.grailsVersion = grailsVersion
        onSetGrailsVersion.execute(grailsVersion)
    }

    //Set the Groovy version and execute the configuratio callback the configures the Groovy resolution
    //strategy on the project
    public void setGroovyVersion(String groovyVersion) {
        if (this.groovyVersion != null) {
            throw new InvalidUserDataException("The 'groovyVersion' property can only be set once")
        }
        this.groovyVersion = groovyVersion
        onSetGroovyVersion.execute(groovyVersion)
    }

    public void onSetGrailsVersion(Action<String> action) {
        onSetGrailsVersion.add(action)
    }

    public void onSetGroovyVersion(Action<String> action) {
        onSetGroovyVersion.add(action)
    }

    @Override
    public String getGrailsVersion() {
        return this.grailsVersion
    }

    public String getGroovyVersion() {
        return this.groovyVersion
    }

    @Override
    public String getSpringLoadedVersion() {
        return springLoadedVersion
    }

    @Override
    public void setSpringLoadedVersion(String springLoadedVersion) {
        this.springLoadedVersion = springLoadedVersion
    }

    public boolean isPluginProject() {
        for (File file : getProjectDir().listFiles()) {
            if (file.getName().endsWith("GrailsPlugin.groovy")) {
                return true
            }
        }
        return false
    }

    /**
     * Configures the Grails central repositories on the project:
     * repositories {
     *    grails.central()
     * }
     *
     */
    public MavenArtifactRepository central() {
        project.repositories.maven { MavenArtifactRepository repository ->
            repository.url = 'http://repo.grails.org/grails/repo'
            repository.name = 'Grails Central'
        }
    }

}
