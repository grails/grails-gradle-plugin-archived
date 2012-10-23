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

import org.gradle.api.Action;
import org.gradle.api.logging.LogLevel;
import org.gradle.internal.Factory;
import org.gradle.internal.UncheckedException;
import org.gradle.messaging.remote.ObjectConnection;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.grails.launcher.context.GrailsLaunchContext;

import java.util.concurrent.CountDownLatch;

public class ForkingGrailsLauncher {

    private final Factory<WorkerProcessBuilder> workerProcessBuilderFactory;

    public ForkingGrailsLauncher(Factory<WorkerProcessBuilder> workerProcessBuilderFactory) {
        this.workerProcessBuilderFactory = workerProcessBuilderFactory;
    }

    public int launch(GrailsLaunchContext launchContext, LogLevel logLevel, Action<JavaForkOptions> javaForkOptionsAction
    ) {
        WorkerProcessBuilder builder = workerProcessBuilderFactory.create();

        builder.setLogLevel(logLevel);
        builder.worker(new GrailsServer());

        JavaExecHandleBuilder javaCommand = builder.getJavaCommand();
        if (javaForkOptionsAction != null) {
            javaForkOptionsAction.execute(javaCommand);
        }

        WorkerProcess workerProcess = builder.build();

        CountDownLatch latch = new CountDownLatch(1);
        LatchBackedGrailsForkHandle forkHandle = new LatchBackedGrailsForkHandle(latch);

        workerProcess.start();
        ObjectConnection connection = workerProcess.getConnection();

        connection.addIncoming(GrailsForkHandle.class, forkHandle);
        GrailsWorkerHandle workerHandle = connection.addOutgoing(GrailsWorkerHandle.class);

        workerHandle.launch(launchContext);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }

        Throwable exception = forkHandle.getExecutionException();
        if (exception != null) {
            throw UncheckedException.throwAsUncheckedException(exception);
        }

        return forkHandle.getExitCode();
    }

}
