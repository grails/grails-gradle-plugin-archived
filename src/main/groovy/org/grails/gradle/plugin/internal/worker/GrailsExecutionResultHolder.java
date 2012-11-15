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

import java.util.Collections;
import java.util.Map;
import java.util.concurrent.CountDownLatch;

public class GrailsExecutionResultHolder implements Action<Object> {

    private final CountDownLatch latch;
    private Map<String, Object> result = Collections.emptyMap();

    public GrailsExecutionResultHolder(CountDownLatch latch) {
        this.latch = latch;
    }

    @Override
    public void execute(Object result) {
        //noinspection unchecked
        this.result = (Map<String, Object>) result;
        latch.countDown();
    }

    public Integer getExitCode() {
        return (Integer) result.get("exitCode");
    }

    public Throwable getExecutionException() {
        return (Throwable) result.get("executionException");
    }

    public Throwable getInitialisationException() {
        return (Throwable) result.get("initialisationException");
    }
}
