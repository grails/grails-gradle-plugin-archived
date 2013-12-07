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

package org.grails.gradle.plugin.integ

import org.gradle.GradleLauncher
import org.gradle.StartParameter
import org.gradle.api.Task
import org.gradle.api.execution.TaskExecutionListener
import org.gradle.api.tasks.TaskState
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class IntegSpec extends Specification {
    @Rule final TemporaryFolder dir = new TemporaryFolder(new File('build'))

    static class ExecutedTask {
        Task task
        TaskState state
    }

    List<ExecutedTask> executedTasks = []

    GradleLauncher launcher(String... args) {
        StartParameter startParameter = GradleLauncher.createStartParameter(args)
        startParameter.setProjectDir(dir.root)
        GradleLauncher launcher = GradleLauncher.newInstance(startParameter)
        executedTasks.clear()
        launcher.addListener(new TaskExecutionListener() {
            void beforeExecute(Task task) {
                IntegSpec.this.executedTasks << new ExecutedTask(task: task)
            }

            void afterExecute(Task task, TaskState taskState) {
                IntegSpec.this.executedTasks.last().state = taskState
                taskState.metaClass.upToDate = taskState.skipMessage == "UP-TO-DATE"
            }
        })
        launcher
    }

    File getBuildFile() {
        file("build.gradle")
    }

    File file(String path) {
        def file = new File(dir.root, path)
        if (!file.exists()) {
            file.parentFile.mkdirs()
            file.createNewFile()
        }
        file
    }

    ExecutedTask task(String name) {
        executedTasks.find { it.task.name == name }
    }

    def setup() {
        buildFile << """
            ext {
                GrailsPlugin = project.class.classLoader.loadClass("org.grails.gradle.plugin.GrailsPlugin")
                GrailsTask = project.class.classLoader.loadClass("org.grails.gradle.plugin.tasks.GrailsTask")
            }
            version = "1.0"

            apply plugin: GrailsPlugin

            repositories {
                grails.central()
            }
        """
    }

}