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
import org.gradle.api.logging.Logger;
import org.gradle.process.JavaExecSpec;
import org.grails.launcher.ForkedGrailsLauncher;
import org.grails.launcher.Main;
import org.grails.launcher.context.GrailsLaunchContext;

import java.io.*;
import java.net.MalformedURLException;
import java.net.URL;
import java.net.URLClassLoader;
import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class GrailsLaunchConfigureAction implements Action<JavaExecSpec> {

    private static final String RELOADED_CLASS = "com.springsource.loaded.ReloadEventProcessorPlugin";

    private final GrailsLaunchContext launchContext;
    private final File contextDestination;
    private final Logger logger;

    public GrailsLaunchConfigureAction(GrailsLaunchContext launchContext, File contextDestination, Logger logger) {
        this.launchContext = launchContext;
        this.contextDestination = contextDestination;
        this.logger = logger;
    }

    @Override
    public void execute(JavaExecSpec javaExec) {
        configureReloadAgent(javaExec); // mutates the launch context

        OutputStream fileOut = null;
        ObjectOutputStream oos = null;
        try {
            fileOut = new FileOutputStream(contextDestination);
            oos = new ObjectOutputStream(fileOut);
            oos.writeObject(launchContext);

            javaExec.setWorkingDir(launchContext.getBaseDir());

            File launcherJar = findJarFile(ForkedGrailsLauncher.class);
            javaExec.classpath(launcherJar);
            javaExec.setMain(Main.class.getName());
            javaExec.args(contextDestination.getAbsolutePath());
        } catch (IOException e) {
            throw new RuntimeException(e);
        } finally {
            try {
                if (oos != null) {
                    oos.close();
                }
                if (fileOut != null) {
                    fileOut.close();
                }
            } catch (IOException ignore) {

            }
        }
    }

    public void configureReloadAgent(JavaExecSpec exec) {
        List<File> buildDependencies = launchContext.getBuildDependencies();
        List<URL> urls = new ArrayList<URL>(buildDependencies.size());
        for (File file : buildDependencies) {
            try {
                urls.add(file.toURI().toURL());
            } catch (MalformedURLException ignore) {

            }
        }

        URL[] urlsArray = new URL[urls.size()];
        urls.toArray(urlsArray);
        ClassLoader classLoader = new URLClassLoader(urlsArray);
        Class<?> agentClass;
        try {
            agentClass = classLoader.loadClass(RELOADED_CLASS);
        } catch (ClassNotFoundException e) {
            logger.info(String.format("Did not find reload agent class '%s' on bootstrap classpath, reloading is disabled", RELOADED_CLASS));
            return;
        }

        File agentJarFile = findJarFile(agentClass);
        List<File> minusAgent = new ArrayList<File>(launchContext.getBuildDependencies());
        minusAgent.remove(agentJarFile);
        launchContext.setBuildDependencies(minusAgent);

        String agentJarFilePath;
        agentJarFilePath = agentJarFile.getAbsolutePath();

        List<String> newJvmArgs = new ArrayList<String>();
        if(exec.getJvmArgs() != null){
            newJvmArgs.addAll(exec.getJvmArgs());
        }
        newJvmArgs.add(String.format("-javaagent:%s", agentJarFilePath));
        newJvmArgs.add("-noverify");
        exec.setJvmArgs(newJvmArgs);
        exec.systemProperty("springloaded", "profile=grails");
    }

    private File findJarFile(Class targetClass) {
        String absolutePath = targetClass.getResource('/' + targetClass.getName().replace(".", "/") + ".class").getPath();
        String jarPath = absolutePath.substring("file:".length(), absolutePath.lastIndexOf("!"));
        return new File(jarPath);
    }

}
