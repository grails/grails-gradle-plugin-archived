Grails Gradle Plugin
====================

This plugin for Gradle allows you to build Grails projects. To use it, simply include the required JARs via `buildscript {}` and 'apply' the plugin:

````````
buildscript {
  repositories {
    jcenter()
  }
  dependencies {
    classpath "org.grails:grails-gradle-plugin:2.0.0-SNAPSHOT"
  }
}

repositories {
  grails.central() //creates a maven repo for the Grails Central repository (Core libraries and plugins)
}

version "0.1"
group "example"

apply plugin: "grails"

grails {
  groovyVersion = '2.1.7'
  springLoadedVersion "1.1.3"
}

dependencies {
  bootstrap "org.grails:grails-plugin-tomcat:${project.grails.grailsVersion}" // No container is deployed by default, so add this
  compile 'org.grails.plugins:resources:1.2' // Just an example of adding a Grails plugin
}
````````
You must specify the 'grails.grailsVersion' property before executing any Grails commands.

The 'grails.groovyVersion' property is a convenience for Grails 2.3.0, it may not work correctly in earlier
versions, so it's best to not use it with version pre-2.3.0. Declaring 'grails.groovyVersion' will configure a Gradle ResolutionStrategy to modify all requests for 'groovy-all' to be
for the version specified. Additionally, the ResolutionStrategy will change all requests for 'groovy' to be 'groovy-all'

The grails-gradle-plugin will populate the bootstrap, compile, and test classpaths with a base set of dependencies for Grails.
You need to provide a container plugin such as 'tomcat' to the bootstrap classpath to enable the run-app command.

*Warning* If you're using a pre-1.3.5 or pre-1.2.4 version of Grails, you'll need to add this runtime dependency to your project's build file:

    runtime org.aspectj:aspectjrt:1.6.8

Once you have this build file, you can create a Grails application with the 'init' task:

    gradle init

The plugin creates standard tasks that mimic the Java lifecycle:

* clean
* test
* check
* build
* assemble

These tasks are wrapper tasks that declare a dependsOn to Grails specific tasks. This will allow for further build customization.

* clean [grails-clean]
* test [grails-test]
* assemble [grails-war or grails-package-plugin]

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
