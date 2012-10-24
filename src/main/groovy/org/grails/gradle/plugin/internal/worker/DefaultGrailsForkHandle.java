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

package org.grails.gradle.plugin.internal.worker;

public class DefaultGrailsForkHandle implements GrailsForkHandle {

    int exitCode;
    Throwable executionException;
    Throwable initialisationException;

    @Override
    public void onExit(int exitCode) {
        this.exitCode = exitCode;
    }

    @Override
    public void onExecutionException(Throwable executionException) {
        this.executionException = executionException;
    }

    @Override
    public void onInitialisationException(Throwable initialisationException) {
        this.initialisationException = initialisationException;
    }

    public int getExitCode() {
        return exitCode;
    }

    public Throwable getExecutionException() {
        return executionException;
    }

    public Throwable getInitialisationException() {
        return initialisationException;
    }
}
