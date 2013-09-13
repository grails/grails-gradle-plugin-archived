package org.grails.gradle.plugin

import org.gradle.api.Action
import org.gradle.api.internal.file.FileResolver
import org.gradle.api.internal.plugins.DslObject
import org.gradle.api.internal.tasks.DefaultGroovySourceSet
import org.gradle.language.java.internal.DefaultJavaSourceSet
import org.gradle.api.internal.tasks.DefaultSourceSet
import org.gradle.api.internal.tasks.SourceSetCompileClasspath
import org.gradle.api.tasks.SourceSet
import org.gradle.internal.reflect.Instantiator
import org.gradle.language.base.FunctionalSourceSet
import org.gradle.language.base.ProjectSourceSet
import org.gradle.language.jvm.ResourceSet
import org.gradle.language.jvm.internal.DefaultResourceSet

class GrailsSourceSetConfigurator {

    private final Instantiator instantiator
    private final FileResolver fileResolver

    GrailsSourceSetConfigurator(Instantiator instantiator, FileResolver fileResolver) {
        this.instantiator = instantiator
        this.fileResolver = fileResolver
    }

    void configure(ProjectSourceSet projectSourceSet, GrailsProject project) {

        project.sourceSets.all(new Action<SourceSet>() {
            public void execute(final SourceSet sourceSet) {
                FunctionalSourceSet functionalSourceSet = projectSourceSet.create(sourceSet.name)
                SourceSetCompileClasspath compileClasspath = new SourceSetCompileClasspath(sourceSet)
                DefaultJavaSourceSet javaSourceSet = instantiator.newInstance(DefaultJavaSourceSet.class, "java", sourceSet.getJava(), compileClasspath, functionalSourceSet)
                functionalSourceSet.add(javaSourceSet)
                ResourceSet resourceSet = instantiator.newInstance(DefaultResourceSet.class, "resources", sourceSet.getResources(), functionalSourceSet)
                functionalSourceSet.add(resourceSet)

                DefaultGroovySourceSet groovySourceSet = new DefaultGroovySourceSet(((DefaultSourceSet) sourceSet).displayName, fileResolver)
                new DslObject(sourceSet).getConvention().getPlugins().put("groovy", groovySourceSet)
                sourceSet.getAllJava().source(groovySourceSet.getGroovy())
                sourceSet.getAllSource().source(groovySourceSet.getGroovy())
            }
        })

        createMainSourceSet(project)
        createTestSourceSet(project)
    }

    void createMainSourceSet(GrailsProject project) {
        project.sourceSets {
            main {
                groovy {
                    srcDirs = [
                            'grails-app/conf',
                            'grails-app/controllers',
                            'grails-app/domain',
                            'grails-app/services',
                            'grails-app/taglib',
                            'grails-app/utils',
                            'src/groovy',
                            'scripts'
                    ]
                    filter {
                        exclude 'grails-app/conf/hibernate'
                        exclude 'grails-app/conf/spring'
                    }
                }
                resources {
                    srcDirs = [
                            'grails-app/conf/hibernate',
                            'grails-app/conf/spring',
                            'grails-app/views',
                            'web-app'
                    ]
                }
                java {
                    srcDirs = [
                            'src/java'
                    ]
                }
            }
        }
    }

    void createTestSourceSet(GrailsProject project) {
        project.sourceSets {
            test {
                groovy {
                    srcDirs = [
                            'test/functional',
                            'test/integration',
                            'test/unit'
                    ]
                }
            }
        }
    }
}
