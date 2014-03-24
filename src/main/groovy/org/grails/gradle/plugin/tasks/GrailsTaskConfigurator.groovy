package org.grails.gradle.plugin.tasks

import org.gradle.api.DefaultTask
import org.gradle.api.Project
import org.gradle.api.Task
import org.gradle.api.plugins.BasePlugin
import org.gradle.api.plugins.JavaBasePlugin
import org.gradle.api.plugins.JavaPlugin
import org.grails.gradle.plugin.GrailsProject

/**
 * Configures the default Grails tasks and wires them into the standard build process:
 * ('build', 'check', 'test', 'assemble')
 */
class GrailsTaskConfigurator {

    public static final String GRAILS_CLEAN_TASK = 'grails-clean'
    public static final String GRAILS_INIT_TASK = 'init'
    public static final String GRAILS_INIT_PLUGIN_TASK = 'init-plugin'
    public static final String GRAILS_TEST_TASK = 'grails-test-app'
    public static final String GRAILS_RUN_TASK = 'grails-run-app'
    public static final String GRAILS_PACKAGE_PLUGIN_TASK = 'grails-package-plugin'
    public static final String GRAILS_WAR_TASK = 'grails-war'

    void configure(Project project, GrailsProject grailsProject) {
        //Setup some tasks that mimic the Java build pattern
        configureJavaStyleTasks(project)

        // Setup the Grails-specific tasks
        configureGrailsTasks(grailsProject, project)

        project.tasks.findByName(BasePlugin.CLEAN_TASK_NAME).dependsOn(project.tasks.findByName(GRAILS_CLEAN_TASK))
        project.tasks.findByName(JavaPlugin.TEST_TASK_NAME).dependsOn(project.tasks.findByName(GRAILS_TEST_TASK))
    }

    /**
     * Wire up the Grails project into the standard Gradle Java build flow (mimic the Java Plugin)
     */
    private void configureJavaStyleTasks(Project project) {
        configureCheck(project)
        configureBuild(project)
        configureTest(project)
    }

    /**
     * Wire up all of the Grails Gradle tasks.
     * @param grailsProject The {@link GrailsProject}.
     * @param project The Gradle {@link Project}.
     */
    private void configureGrailsTasks(GrailsProject grailsProject, Project project) {
        createGrailsTask(project, GrailsInitTask, GRAILS_INIT_TASK, 'create-app', 'Creates a new Grails application in the current directory.')
        createGrailsTask(project, GrailsInitTask, GRAILS_INIT_PLUGIN_TASK, 'create-plugin', 'Creates a new Grails plugin in the current directory.')
        createGrailsTask(project, GrailsTask, GRAILS_CLEAN_TASK, 'clean', 'Executes Grails clean.')
        createGrailsTask(project, GrailsTask, GRAILS_RUN_TASK, 'run-app', 'Starts the Grails application.')
        createGrailsTask(project, GrailsTestTask, GRAILS_TEST_TASK, 'test-app', 'Executes Grails tests.')

        //Depending on the project type, configure either the package-plugin or war tasks
        //as the assemble task.  Then, set up the proper assemble task and adds it's
        // artifact to the configuration
        if(grailsProject.pluginProject) {
            configureAssemble(project, createGrailsTask(project, GrailsPluginPackageTask, GRAILS_PACKAGE_PLUGIN_TASK, 'package-plugin', 'Packages a grails plugin.'))
        } else {
            configureAssemble(project, createGrailsTask(project, GrailsWarTask, GRAILS_WAR_TASK, 'war', 'Generates the application WAR file.'))
        }

    }

    /**
     * Creates and registers a new Gradle task with the {@link Project}.
     * @param project The Gradle {@link Project}.
     * @param taskType The class of the task to be created.
     * @param taskName The name of the task.
     * @param command The Grails command.
     * @param description The description of the Grails task.
     */
    private Task createGrailsTask(Project project, Class taskType, String taskName, String command, String description) {
        // Some of the Grails tasks replace existing Gradle tasks.  Therefore,
        // if we find a match, remove it first so that we can re-create it
        // (or create it for the first time).
        if (project.tasks.findByName(taskName)) {
            project.tasks.removeByName(taskName)
        }

        Task task = project.tasks.create(taskName, taskType)
        task.command = command
        task.description = description

        if (task.hasProperty('args') && project.hasProperty(GrailsTask.GRAILS_ARGS_PROPERTY)) {
            task.args = createArgs(task, project)
        }
        if (task.hasProperty('env') && project.hasProperty(GrailsTask.GRAILS_ENV_PROPERTY)) {
            task.env = project.property(GrailsTask.GRAILS_ENV_PROPERTY)
        }
        if (task.hasProperty('jvmOptions')) {
            if(project.hasProperty(GrailsTask.GRAILS_JVM_ARGS_PROPERTY)) {
                task.jvmOptions.setAllJvmArgs(project.property(GrailsTask.GRAILS_JVM_ARGS_PROPERTY).tokenize())
            }

            if(project.hasProperty(GrailsTask.GRAILS_DEBUG_PROPERTY)) {
                task.jvmOptions.debug = Boolean.parseBoolean(project.property(GrailsTask.GRAILS_DEBUG_PROPERTY))
            }
        }

        task
    }

    /**
     * Add the 'check' task
     */
    private void configureCheck(Project project) {
        if (!project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)) {
            project.tasks.create(JavaBasePlugin.CHECK_TASK_NAME)
        }
        Task checkTask = project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME)
        checkTask.setDescription("Runs all checks.")
        checkTask.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
    }

    /**
     * Add the 'build' task and wire it to 'check' and 'assemble'
     */
    private void configureBuild(Project project) {
        if (!project.tasks.findByName(JavaBasePlugin.BUILD_TASK_NAME)) {
            project.tasks.create(JavaBasePlugin.BUILD_TASK_NAME, DefaultTask.class)
        }
        DefaultTask buildTask = project.tasks.findByName(JavaBasePlugin.BUILD_TASK_NAME)
        buildTask.setDescription("Assembles and tests this project.")
        buildTask.setGroup(BasePlugin.BUILD_GROUP)
        buildTask.dependsOn(BasePlugin.ASSEMBLE_TASK_NAME)
        buildTask.dependsOn(JavaBasePlugin.CHECK_TASK_NAME)
    }

    /**
     * Add the 'test' task and wire it to 'check'
     */
    private void configureTest(Project project) {
        if (!project.tasks.findByName(JavaPlugin.TEST_TASK_NAME)) {
            project.tasks.create(JavaPlugin.TEST_TASK_NAME, DefaultTask.class)
        }
        Task test = project.tasks.findByName(JavaPlugin.TEST_TASK_NAME)
        project.tasks.findByName(JavaBasePlugin.CHECK_TASK_NAME).dependsOn(test)
        test.setDescription("Runs the tests.")
        test.setGroup(JavaBasePlugin.VERIFICATION_GROUP)
    }

    private void configureAssemble(Project project, Task grailsAssembleTask) {
        project.tasks.findByName(BasePlugin.ASSEMBLE_TASK_NAME).dependsOn grailsAssembleTask
        project.configurations.default.extendsFrom(project.configurations.runtime)
        project.afterEvaluate {
            project.artifacts.add('runtime', grailsAssembleTask.outputFile) {
                type grailsAssembleTask.outputFile.path.tokenize('.').last()
                builtBy grailsAssembleTask
            }
        }
    }

    /**
     * Creates the command line argument string for the Grails command.  Note that
     * if the task already contains command line arguments, any arguments found
     * as properties in the project will be appended to the existing arguments.
     * @param task The Gradle tasks that represents the Grails command.
     * @param project The {@link Project} that may contain command line arguments for the
     * 	Grails task.
     * @return The updated Grails command line arguments, if any are present in the project.
     */
    private CharSequence createArgs(Task task, Project project) {
        if(task.args != null && task.args.toString()) {
            StringBuilder builder = new StringBuilder(task.args)
            builder.append(' ')
            builder.append(project.property(GrailsTask.GRAILS_ARGS_PROPERTY))
            builder
        } else {
            project.property(GrailsTask.GRAILS_ARGS_PROPERTY)
        }
    }
}
