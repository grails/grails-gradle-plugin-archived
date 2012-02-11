package org.grails.gradle.plugin.integ

import spock.lang.Unroll

class InitSpec extends IntegSpec {

    @Unroll({"can create grails $grailsVersion project"})
    def "can init project"() {
        given:
        applyPlugin(grailsVersion)

        when:
        launcher("init", "-s").run().rethrowFailure()
        
        then:
        task("init").state.didWork
        
        and:
        file("grails-app").exists()
        
        where:
        grailsVersion << ["1.3.7", "2.0.0"]
    }
    
}
