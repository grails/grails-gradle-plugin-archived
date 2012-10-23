/*
 * Copyright 2012 the original author or authors.
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

package org.grails.gradle.plugin.internal;

import org.gradle.api.Action;
import org.gradle.api.InvalidUserDataException;
import org.gradle.api.Project;
import org.gradle.listener.ActionBroadcast;
import org.grails.gradle.plugin.GrailsProject;

import java.io.File;

public class DefaultGrailsProject implements GrailsProject {

    private final Project project;

    private Object projectDir;
    private Object projectWorkDir;

    private String grailsVersion;
    private ActionBroadcast<String> onSetGrailsVersion = new ActionBroadcast<String>();

    public DefaultGrailsProject(Project project) {
        this.project = project;
    }

    public File getProjectDir() {
        return projectDir == null ? null : project.file(projectDir);
    }

    public void setProjectDir(Object projectDir) {
        this.projectDir = projectDir;
    }

    public File getProjectWorkDir() {
        return projectWorkDir == null ? null : project.file(projectWorkDir);
    }

    public void setProjectWorkDir(Object projectWorkDir) {
        this.projectWorkDir = projectWorkDir;
    }

    @Override
    public void setGrailsVersion(String grailsVersion) {
        if (this.grailsVersion != null) {
            throw new InvalidUserDataException("The 'grailsVersion' property can only be set once");
        }
        this.grailsVersion = grailsVersion;
        onSetGrailsVersion.execute(grailsVersion);
    }

    @Override
    public String getGrailsVersion() {
        return this.grailsVersion;
    }

    public void onSetGrailsVersion(Action<String> action) {
        onSetGrailsVersion.add(action);
    }
}
