/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2023 Stephan Pauxberger
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all
 * copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.blackbuild.groovycps.jenkins

import com.blackbuild.groovycps.tests.GradleIntegrationTest
import org.gradle.testkit.runner.TaskOutcome
import org.intellij.lang.annotations.Language

class JenkinsDependenciesPluginTest extends GradleIntegrationTest {

    String pluginIdToTest = "com.blackbuild.jenkins.dependencies"

    File pluginMapping
    File versionMapping

    @Override
    def setup() {
        new File(testProjectDir, "plugins").mkdirs()
        pluginMapping = new File(testProjectDir, JenkinsDependenciesExtension.DEFAULT_PLUGIN_MAPPINGS)
        versionMapping = new File(testProjectDir, JenkinsDependenciesExtension.DEFAULT_PLUGIN_VERSIONS)
    }

    def "plugins can be registered"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "bla"
    plugin "blub"
}
"""

        when:
        runTask("help")

        then:
        noExceptionThrown()

    }

    def "jenkins version can be overridden"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    jenkinsVersion = "2.319.1"
}
"""
        withVerifyTask '''
        assert project.configurations.jenkinsCore.dependencies.matching { it.name == "jenkins-core" && it.version == "2.319.1" }

'''
        withPluginMapping ''
        withVersionMapping ''


        when:
        runTask("dependencies",  DO_VERIFY_TASK)

        then:
        noExceptionThrown()

    }

    def "plugins can be added"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "blueocean"
}
"""
        withVerifyTask '''
        assert project.configurations.jenkinsPlugins.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean" && it.moduleVersion == "1.27.0" 
        }

'''
        withPluginMapping '''
blueocean=io.jenkins.blueocean:blueocean
'''
        withVersionMapping '''
blueocean=1.27.0
'''

        when:
        runVerifyTask()

        then:
        noExceptionThrown()
    }

    def "plugins jars are added to implementation"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "blueocean"
}
"""
        withVerifyTask '''
        assert project.configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean" && it.moduleVersion == "1.27.0" 
        }
        assert project.configurations.compileClasspath.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean-web" && it.moduleVersion == "1.27.0" 
        }

'''
        withPluginMapping '''
blueocean=io.jenkins.blueocean:blueocean
'''
        withVersionMapping '''
blueocean=1.27.0
'''

        when:
        runVerifyTask()

        then:
        noExceptionThrown()
    }

    def "plugin version can be explicitly overridden."() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "blueocean:1.27.0"
}
"""
        withVerifyTask '''
        assert project.configurations.jenkinsPlugins.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean" && it.moduleVersion == "1.27.0" 
        }
'''

        withPluginMapping '''
blueocean=io.jenkins.blueocean:blueocean
'''
        withVersionMapping '''
blueocean=1.26.0
'''

        when:
        runTask("dependencies",  DO_VERIFY_TASK)
        // runVerifyTask()

        then:
        noExceptionThrown()
    }

    def "plugin can be GA"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "io.jenkins.blueocean:blueocean"
}
"""
        withVerifyTask '''
        assert project.configurations.jenkinsPlugins.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean" && it.moduleVersion == "1.27.0" 
        }
'''

        withPluginMapping '''
blueocean=io.jenkins.blueocean:blueocean
'''
        withVersionMapping '''
blueocean=1.27.0
'''

        when:
        runTask("dependencies",  DO_VERIFY_TASK)
        // runVerifyTask()

        then:
        noExceptionThrown()
    }

    def "plugin can be GAV"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    plugin "io.jenkins.blueocean:blueocean:1.27.0"
}
"""
        withVerifyTask '''
        assert project.configurations.jenkinsPlugins.resolvedConfiguration.firstLevelModuleDependencies.find { 
            it.moduleGroup == "io.jenkins.blueocean" && it.moduleName == "blueocean" && it.moduleVersion == "1.27.0" 
        }
'''

        withPluginMapping '''
blueocean=io.jenkins.blueocean:blueocean
'''
        withVersionMapping '''
blueocean=1.26.0
'''

        when:
        runTask("dependencies",  DO_VERIFY_TASK)
        // runVerifyTask()

        then:
        noExceptionThrown()
    }

    def "plugins are copied to target folder"() {
        given:
        def pluginDir = new File(testProjectDir, "build/jenkins-plugins/test-dependencies")
        withDefaultRepositories()
        withBuild """
jenkins {
    doNotAddJenkinsRepository()
    plugin "job-dsl"
}
"""
        withPlugins([
                "org.jenkins-ci.plugins:job-dsl:1.77",
                "org.jenkins-ci.plugins:structs:1.19",
                "org.jenkins-ci.plugins:script-security:1.54"
        ])

        when:
        runTask("copyJenkinsPlugins", "--stacktrace")

        then:
        new File(pluginDir, "index").exists()
        new File(pluginDir, "index").text.readLines().sort() == ["job-dsl", "structs", "script-security"].sort()
        pluginDir.list().sort().toList() == ["job-dsl.hpi", "structs.hpi", "script-security.hpi", "index"].sort()
    }

    def "test harness is added"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    useTestHarness()
    plugin "job-dsl"
}

dependencies {
    testImplementation 'org.spockframework:spock-core:1.3-groovy-2.4'
}
"""
        withPlugins([
                "org.jenkins-ci.plugins:job-dsl:1.77",
                "org.jenkins-ci.plugins:structs:1.19",
                "org.jenkins-ci.plugins:script-security:1.54"
        ])

        withFile "src/test/groovy/MyTest.groovy",   '''
import spock.lang.Specification
class MyTest extends Specification {

    def 'my test'() {
        expect:
        true
    }
}
'''


        withVerifyTask '''
        assert project.configurations.jenkinsCore.dependencies.matching { it.name == "jenkins-core" && it.version == "2.375.1" }
        assert project.configurations.jenkinsTestHarness.dependencies.matching { it.name == "jenkins-test-harness" && it.version == "2129.v09f309d2339c" }
        assert project.configurations.testRuntimeClasspath.resolve().contains(project.layout.buildDirectory.dir("jenkins-plugins").get().asFile)
'''

        when:
        def result = runTask("test", DO_VERIFY_TASK)

        then:
        noExceptionThrown()
        result.task(":copyJenkinsPlugins").outcome == TaskOutcome.SUCCESS
    }

    def "war file is added and resolved"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    useTestHarness()
    plugin "job-dsl"
}

dependencies {
    testImplementation 'org.spockframework:spock-core:1.3-groovy-2.4'
}
"""
        withPlugins([
                "org.jenkins-ci.plugins:job-dsl:1.77",
                "org.jenkins-ci.plugins:structs:1.19",
                "org.jenkins-ci.plugins:script-security:1.54"
        ])

        withFile "src/test/groovy/MyTest.groovy",   '''
import spock.lang.Specification
class MyTest extends Specification {

    def 'my test'() {
        expect:
        System.getProperty("jth.jenkins-war.path")
        new File(System.getProperty("jth.jenkins-war.path")).exists()
        System.getProperty("buildDirectory")
    }
}
'''


        withVerifyTask '''
        assert project.configurations.jenkinsWar.state == Configuration.State.RESOLVED
        def warFiles = project.configurations.jenkinsWar.resolve()
        assert warFiles.size() == 1
        assert warFiles.first().name == "jenkins-war-2.375.1.war"
        assert warFiles.first().isFile()
        def warSet = project.configurations.jenkinsWar.dependencies.matching { it.name == "jenkins-war" && it.version == "2.375.1" }
        assert warSet.size() == 1
        assert project.configurations.testRuntimeClasspath.state == Configuration.State.RESOLVED
        assert project.configurations.testRuntimeClasspath.resolve().contains(project.layout.buildDirectory.dir("jenkins-plugins").get().asFile)
'''

        when:
        def result = runTask("test", DO_VERIFY_TASK, "--stacktrace")

        then:
        noExceptionThrown()
        result.task(":copyJenkinsPlugins").outcome == TaskOutcome.SUCCESS
    }

    def "BUG: fails on existing plugin folder"() {
        given:
        withDefaultRepositories()
        withBuild """
jenkins {
    doNotAddJenkinsRepository()
    plugin "job-dsl"
}
"""
        withPlugins([
                "org.jenkins-ci.plugins:job-dsl:1.77",
                "org.jenkins-ci.plugins:structs:1.19",
                "org.jenkins-ci.plugins:script-security:1.54"
        ])

        when:
        runTask("copyJenkinsPlugins")

        then:
        noExceptionThrown()

        when:
        runTask("copyJenkinsPlugins")

        then:
        noExceptionThrown()
    }

    def withPluginMapping(@Language("Properties") String content) {
        pluginMapping << content
    }
    def withVersionMapping(@Language("Properties") String content) {
        versionMapping << content
    }

    def withPlugins(List<String> plugins) {
        withPluginMapping(plugins
                .collect { gav -> gav.tokenize(":") }
                .collect { gav -> "${gav[1]}=${gav[0]}:${gav[1]}" }
                .join("\n"))
        withVersionMapping(plugins
                .collect { gav -> gav.tokenize(":") }
                .collect { gav -> "${gav[1]}=${gav[2]}" }
                .join("\n"))
    }

    void withDefaultRepositories() {
        buildFile << '''
repositories {
    mavenCentral()
    mavenLocal()
    maven {
        url "https://repo.jenkins-ci.org/releases/"
    }
}
'''
    }
}
