package org.grails.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.grails.gradle.plugin.GrailsProject


class GrailsTaskConfigurator {

    public static final String GRAILS_CLEAN_TASK = 'grails-clean'
    public static final String GRAILS_INIT_TASK = 'init'
    public static final String GRAILS_TEST_TASK = 'grails-test-app'
    public static final String GRAILS_RUN_TASK = 'grails-run-app'
    public static final String GRAILS_PACKAGE_PLUGIN_TASK = 'grails-package-plugin'
    public static final String GRAILS_WAR_TASK = 'grails-war'

    void configure(Project project, GrailsProject grailsProject) {
        project.tasks.create(GRAILS_INIT_TASK, GrailsInitTask)

        def grailsClean = project.tasks.create(GRAILS_CLEAN_TASK, GrailsCleanTask)
        project.tasks.getByName(BasePlugin.CLEAN_TASK_NAME).dependsOn grailsClean

        def grailsAssemble = grailsProject.pluginProject ?
                project.tasks.create(GRAILS_PACKAGE_PLUGIN_TASK, GrailsPackagePluginTask) :
                project.tasks.create(GRAILS_WAR_TASK, GrailsWarTask)

        project.tasks.getByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn grailsAssemble

        if (!grailsProject.isPluginProject()) {
            project.tasks.create(GRAILS_RUN_TASK, GrailsRunAppTask)
        }

        def grailsTest = project.tasks.create(GRAILS_TEST_TASK, GrailsTestTask)

        project.tasks.addRule("Grails command") { String name ->
            if (name.startsWith(GrailsTask.GRAILS_TASK_PREFIX)) {
                project.task(name, type: GrailsTask) {
                    command = (name - GrailsTask.GRAILS_TASK_PREFIX)
                    if (project.hasProperty(GrailsTask.GRAILS_ARGS_PROPERTY)) {
                        args = project.property(GrailsTask.GRAILS_ARGS_PROPERTY)
                    }
                    if (project.hasProperty(GrailsTask.GRAILS_ENV_PROPERTY)) {
                        env = project.property(GrailsTask.GRAILS_ENV_PROPERTY)
                    }
                    if (project.hasProperty(GrailsTask.GRAILS_DEBUG_PROPERTY)) {
                        jvmOptions.debug = Boolean.parseBoolean(project.property(GrailsTask.GRAILS_DEBUG_PROPERTY))
                    }
                }
            }
        }

        configureJavaStyleTasks(project)

        project.tasks.getByName(JavaPlugin.TEST_TASK_NAME).dependsOn grailsTest
    }

    private void configureJavaStyleTasks(Project project) {
        configureCheck(project)
        configureBuild(project)
        configureTest(project)
    }

    private void configureCheck(Project project) {
        Task checkTask = project.getTasks().create(JavaBasePlugin.CHECK_TASK_NAME);
        checkTask.setDescription("Runs all checks.");
        checkTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
    }

    private void configureBuild(Project project) {
        DefaultTask buildTask = project.getTasks().create(JavaBasePlugin.BUILD_TASK_NAME, DefaultTask.class);
        buildTask.setDescription("Assembles and tests this project.");
        buildTask.setGroup(BasePlugin.BUILD_GROUP);
        buildTask.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME);
        buildTask.dependsOn(JavaBasePlugin.CHECK_TASK_NAME);
    }

    private void configureTest(Project project) {
        Task test = project.tasks.create(JavaPlugin.TEST_TASK_NAME, DefaultTask.class)
        project.getTasks().getByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(test);
        test.setDescription("Runs the tests.");
        test.setGroup(JavaBasePlugin.VERIFICATION_GROUP);
    }
}
