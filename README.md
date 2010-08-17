Grails Gradle Plugin
====================

This plugin for Gradle allows you to build Grails projects. To use it, simply include the required JARs via `buildscript {}` and 'apply' the plugin:

    buildscript {
        repositories {
            mavenCentral()
            mavenRepo urls: "http://repository.jboss.org/maven2/"
        }

        dependencies {
            classpath "org.grails:grails-gradle-plugin:1.0",
                      "org.grails:grails-bootstrap:1.3.4"
        }
    }

    apply plugin: "grails"

    repositories {
        mavenCentral()
        mavenRepo urls: "http://repository.jboss.org/maven2/"
    }

    dependencies {
        compile "org.grails:grails-crud:1.3.4",
                "org.grails:grails-gorm:1.3.4"
    }

You must include a version of the 'grails-bootstrap' artifact in the 'classpath' configuration. You should also add whichever Grails artifacts you need. 'grails-crud' and 'grails-gorm' will give you everything you need for a standard Grails web application.

Once you have this build file, you can create a Grails application with the 'init' task:

    gradle init

Other standard tasks include:

* clean
* compile
* test
* assemble

You can also access any Grails command by prefixing it with 'grails-'. For example, to run the application:

    gradle grails-run-app

If you want to pass in some arguments, you can do so via the `args` project property:

    gradle -Pargs='--inplace solr' grails-create-plugin

You can also change the environment via the `env` project property:

    gradle -Penv=prod grails-run-app

*Warning* Version 1.0 of the plugin does not allow you to execute multiple tasks in one command line. So `gradle clean test` will fail even if `clean` and `test` individually succeed.
