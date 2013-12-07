Grails Gradle Plugin
====================

This plugin for Gradle allows you to build Grails projects. To use it, simply include the required JARs via `buildscript {}` and 'apply' the plugin:

````````
buildscript {
  repositories {
    mavenRepo(name: "Grails Repo", url: "http://repo.grails.org/grails/repo")
  }
  dependencies {
    classpath "org.grails:grails-gradle-plugin:2.0.0-SNAPSHOT"
  }
}

repositories {
  mavenRepo(name: "Grails Repo", url: "http://repo.grails.org/grails/repo")
}

version "0.1"
group "example"

apply plugin: "grails"

grails {
  grailsVersion "2.2.3"
  springLoadedVersion "1.1.3"
}

dependencies {
  bootstrap "org.grails:grails-plugin-tomcat:${project.grails.grailsVersion}"
}
````````

The grails-gradle-plugin will populate the bootstrap, compile, and test classpaths with a base set of dependencies for Grails.
You need to provide a container plugin such as 'tomcat' to the bootstrap classpath to enable the run-app command.

*Warning* If you're using a pre-1.3.5 or pre-1.2.4 version of Grails, you'll need to add this runtime dependency to your project's build file:

    runtime org.aspectj:aspectjrt:1.6.8

Once you have this build file, you can create a Grails application with the 'init' task:

    gradle init

Other standard tasks include:

* clean
* compile
* test
* assemble

You can also access any Grails command by prefixing it with 'grails-'. For example, to run the application:

    gradle grails-run-app

If you want to pass in some arguments, you can do so via the `grailsArgs` project property:

    gradle -PgrailsArgs='--inplace solr' grails-create-plugin

You can also change the environment via the `env` project property:

    gradle -PgrailsEnv=prod grails-run-app

You can execute multiple Grails commands in a single step, but bear in mind that if you are passing `grailsEnv` or `grailsArgs` then each of the
commands will execute with the same values.

Troubleshooting
===============

* Caused by: org.apache.tools.ant.BuildException: java.lang.NoClassDefFoundError: org/apache/commons/cli/Options

  This happens if your project depends on the 'groovy' JAR rather than 'groovy-all'. Change your dependency to the latter and all will be well.

* Classloading issues, casting proxy instances to their corresponding interface

  This can be a sign of a corrupted Spring-Loaded cache directory.  The plugin has spring-loaded cache in `$HOME/.grails/.slcache` - try cleaning that directory
