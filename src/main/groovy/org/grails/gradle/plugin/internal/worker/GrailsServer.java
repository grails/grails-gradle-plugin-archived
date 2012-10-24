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
import org.gradle.internal.UncheckedException;
import org.gradle.process.internal.WorkerProcessContext;
import org.gradle.util.FilteringClassLoader;
import org.grails.launcher.GrailsLauncher;
import org.grails.launcher.ReflectiveGrailsLauncher;
import org.grails.launcher.context.GrailsLaunchContext;
import org.grails.launcher.rootloader.RootLoader;
import org.grails.launcher.rootloader.RootLoaderFactory;

import java.io.Serializable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.atomic.AtomicBoolean;

public class GrailsServer implements Action<WorkerProcessContext>, Serializable {

    @Override
    public void execute(WorkerProcessContext context) {
        final GrailsForkHandle forkHandle = context.getServerConnection().addOutgoing(GrailsForkHandle.class);

        final CountDownLatch latch = new CountDownLatch(1);
        GrailsWorkerHandle workerHandle = new GrailsWorkerHandle() {
            @Override
            public void launch(final GrailsLaunchContext launchContext) {
                try {
                    FilteringClassLoader parentLoader = new FilteringClassLoader(ClassLoader.getSystemClassLoader());
                    RootLoader rootLoader = new RootLoaderFactory().create(launchContext, parentLoader);
                    GrailsLauncher grailsLauncher = new ReflectiveGrailsLauncher(rootLoader);

                    try {
                        int exitCode = grailsLauncher.launch(launchContext);
                        forkHandle.onExit(exitCode);
                    } catch (Throwable e) {
                        forkHandle.onExecutionException(e);
                    }
                } catch (Throwable e) {
                    e.printStackTrace();
                    forkHandle.onInitialisationException(e);
                } finally {
                    latch.countDown();
                }
            }
        };

        context.getServerConnection().addIncoming(GrailsWorkerHandle.class, workerHandle);

        try {
            latch.await();
        } catch (InterruptedException e) {
            throw UncheckedException.throwAsUncheckedException(e);
        }
    }

}
