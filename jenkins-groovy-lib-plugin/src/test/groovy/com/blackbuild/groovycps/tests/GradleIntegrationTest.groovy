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
package com.blackbuild.groovycps.tests

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.GradleRunner
import org.intellij.lang.annotations.Language
import spock.lang.Specification
import spock.lang.TempDir

abstract class GradleIntegrationTest extends Specification {

    public static final String DO_VERIFY_TASK = "doVerify"
    @TempDir File testProjectDir
    File settingsFile
    File buildFile

    def setup() {
        settingsFile = new File(testProjectDir, 'settings.gradle')
        buildFile = new File(testProjectDir, 'build.gradle')
        settingsFile << "rootProject.name = 'pipeline'"
        buildFile << """
        plugins {
            id '$pluginIdToTest'
        }
        """
    }

    abstract String getPluginIdToTest()

    void withVerifyTask(@Language("gradle") String code) {
        buildFile << """
        tasks.register('$DO_VERIFY_TASK') {
            doLast {
                $code
            }
        }
        """
    }

    void withBuild(@Language("gradle") String code) {
        buildFile << """
            $code
"""
    }

    protected BuildResult runTask(String... tasks) {
        return GradleRunner.create()
                .withProjectDir(testProjectDir)
                .withArguments(tasks)
                .withDebug(true)
                .withPluginClasspath()
                .forwardOutput()
                .build()
    }

    protected BuildResult runVerifyTask() {
        runTask(DO_VERIFY_TASK)
    }

}
