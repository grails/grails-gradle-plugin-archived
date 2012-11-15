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
import org.gradle.process.ExecResult;
import org.gradle.process.JavaForkOptions;
import org.gradle.process.internal.ExecException;
import org.gradle.process.internal.JavaExecHandleBuilder;
import org.gradle.process.internal.WorkerProcess;
import org.gradle.process.internal.WorkerProcessBuilder;
import org.grails.launcher.context.GrailsLaunchContext;

import java.util.Collections;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

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

        final WorkerProcess workerProcess = builder.build();
        final CountDownLatch latch = new CountDownLatch(1);

        final GrailsExecutionResultHolder resultHolder = new GrailsExecutionResultHolder(latch);

        workerProcess.start();
        ObjectConnection connection = workerProcess.getConnection();

        GrailsWorkerHandle workerHandle = connection.addOutgoing(GrailsWorkerHandle.class);
        connection.addIncoming(Action.class, resultHolder);

        final AtomicBoolean alreadyStopped = new AtomicBoolean(false);

        Thread watchForStopThread = new Thread("Gradle Process Stop Watcher") {
            @Override
            public void run() {
                try {
                    ExecResult result = workerProcess.waitForStop();
                    alreadyStopped.set(true);
                    if (resultHolder.getExitCode() == null) {
                        resultHolder.execute(Collections.singletonMap("exitCode", result.getExitValue()));
                    }
                } catch (ExecException e) {
                    if (resultHolder.getExitCode() == null) { // forcibly exited
                        resultHolder.execute(Collections.singletonMap("exitCode", 1));
                    }
                    alreadyStopped.set(true);
                }
            }
        };
        watchForStopThread.setDaemon(true);
        watchForStopThread.start();

        workerHandle.launch(launchContext);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }

        Throwable exception = resultHolder.getInitialisationException();
        if (exception == null) {
            exception = resultHolder.getExecutionException();
        }

        try {
            if (exception == null) {
                return resultHolder.getExitCode();
            } else {
                throw UncheckedException.throwAsUncheckedException(exception);
            }
        } finally {
            if (!alreadyStopped.get()) {
                workerHandle.stop();
            }
        }
    }

}
