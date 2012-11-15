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
import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GrailsServer implements Action<WorkerProcessContext>, Serializable {

    @Override
    public void execute(WorkerProcessContext context) {
        @SuppressWarnings("unchecked")
        final Action<Map<String, ?>> forkHandle = context.getServerConnection().addOutgoing(Action.class);

        @SuppressWarnings("Convert2Diamond")
        final Map<String, Object> result = new HashMap<String, Object>(1);

        final CountDownLatch latch = new CountDownLatch(1);

        GrailsWorkerHandle workerHandle = new GrailsWorkerHandle() {
            @Override
            public void launch(final GrailsLaunchContext launchContext) {
                try {
                    FilteringClassLoader parentLoader = new FilteringClassLoader(ClassLoader.getSystemClassLoader());
                    RootLoader rootLoader = new RootLoaderFactory().create(launchContext, parentLoader);
                    GrailsLauncher grailsLauncher = new ReflectiveGrailsLauncher(rootLoader);

                    System.setProperty("grails.console.enable.terminal", "false");
                    System.setProperty("grails.console.enable.interactive", "false");

                    try {
                        result.put("exitCode", grailsLauncher.launch(launchContext));
                    } catch (Throwable e) {
                        result.put("executionException", e);
                    }
                } catch (Throwable e) {
                    result.put("initialisationException", e);
                }

                forkHandle.execute(result);
            }

            @Override
            public void stop() {
                latch.countDown();
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
