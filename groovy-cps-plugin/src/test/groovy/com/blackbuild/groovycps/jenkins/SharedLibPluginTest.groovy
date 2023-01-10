/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2015-2022 Stephan Pauxberger
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
import org.intellij.lang.annotations.Language

class SharedLibPluginTest extends GradleIntegrationTest {

    String pluginIdToTest = "com.blackbuild.jenkins.shared-lib"

    File pluginMapping
    File versionMapping

    @Override
    def setup() {
        new File(testProjectDir, "plugins").mkdirs()
        pluginMapping = new File(testProjectDir, SharedLibExtension.DEFAULT_PLUGIN_MAPPINGS)
        versionMapping = new File(testProjectDir, SharedLibExtension.DEFAULT_PLUGIN_VERSIONS)
    }

    def "main and test srcDirs are correctly set"() {
        given:
        withVerifyTask '''
            assert project.sourceSets.main.groovy.srcDirs.collect { projectDir.relativePath(it) } == ['src', 'vars', 'jenkins', ]
            assert project.sourceSets.main.resources.srcDirs.collect { projectDir.relativePath(it) } == ['gdsl','resources']
            assert project.sourceSets.test.groovy.srcDirs.collect { projectDir.relativePath(it) } == ['test']
            assert project.sourceSets.test.resources.srcDirs.collect { projectDir.relativePath(it) } == ['testResources']
        '''

        when:
        runVerifyTask()

        then:
        noExceptionThrown()
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

    def "plugins can be explicitly overridden."() {
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

    def withPluginMapping(@Language("Properties") String content) {
        pluginMapping << content
    }
    def withVersionMapping(@Language("Properties") String content) {
        versionMapping << content
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
