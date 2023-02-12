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
package com.blackbuild.groovycps.jenkins.tests

import com.cloudbees.groovy.cps.Continuation
import com.cloudbees.groovy.cps.impl.CpsCallableInvocation
import hudson.AbortException
import hudson.model.Job
import hudson.model.Run
import org.jenkinsci.plugins.workflow.cps.CpsScript
import org.jenkinsci.plugins.workflow.support.steps.build.RunWrapper
import org.junit.Rule
import org.junit.rules.TemporaryFolder
import spock.lang.Specification

abstract class AbstractCpsTest extends Specification {

    @Rule TemporaryFolder temporaryFolder = new TemporaryFolder()

    CpsScript script
    Map<String, String> scriptEnv = [:]
    Map<String, Object> jobParams = [:]
    StringWriter outputWriter = new StringWriter()
    Job job = Mock()
    Run rawBuild = GroovyMock() {
        getParent() >> job
    }
    RunWrapper currentBuild = GroovyMock() {
        getRawBuild() >> rawBuild
    }

    int blockIndent = 0


    def setup() {
        script = GroovyMock(CpsScript) {
            getBinding() >> new Binding()
            getEnv() >> scriptEnv
            getParams() >> jobParams
            echo(_) >> { CharSequence message ->
                println message
                outputWriter.println(message)
            }
            //noinspection GroovyAssignabilityCheck
            getCurrentBuild() >> currentBuild
            //noinspection GroovyAssignabilityCheck
            dir(_, _) >> { String path, Closure body -> namedBlock("DIR: $path", body)}
            //noinspection GroovyAssignabilityCheck
            error(_) >> { String message -> throw new AbortException(message) }
            stage(_, _) >> { String name, Closure body -> namedBlock("Stage $name", body) }
            timeout(_, _) >> { Map args, Closure body -> namedBlock("timeout", body) }
            withEnv(_, _) >> { List args, Closure body -> namedBlock("withEnv $args", body) }
        }

        // script.env is actually an instance of EnvActionImpl, not a map. Setting of versions is done via setProperty
        // meta method, so we simply monkeypatch that one
        scriptEnv.getMetaClass().setProperty = { String name, Object value -> put(name, value) }
    }

    protected Object namedBlock(String name, Closure body) {
        try {
            println "${" " * blockIndent * 2}$name: {"
            blockIndent++
            execute body
        } finally {
            println " " * blockIndent * 2
            blockIndent--
        }
    }

    /**
     * Wrapper method for CPS code.
     * @param code
     */
    def <T> T execute(Closure<T> code) {
        try {
            return code.call()
        } catch (CpsCallableInvocation e) {
            return e.invoke(null, null, Continuation.HALT).run(100000).replay() as T
        }
    }

    List<String> getLog() {
        outputWriter.toString().readLines()
    }

    void withEnv(Map<String, String> env) {
        scriptEnv << env
    }

    void withParams(Map<String, Object> params) {
        jobParams << params
    }
}
