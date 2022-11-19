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
package com.blackbuild.groovy.cps.astchecker

import com.cloudbees.groovy.cps.CpsTransformer
import groovy.transform.TypeChecked
import org.codehaus.groovy.control.CompilerConfiguration
import org.codehaus.groovy.control.MultipleCompilationErrorsException
import org.codehaus.groovy.control.customizers.ASTTransformationCustomizer
import org.codehaus.groovy.control.customizers.ImportCustomizer
import spock.lang.Specification


class CpsCheckSpec extends Specification {

    ClassLoader oldLoader
    GroovyClassLoader loader
    Class<?> clazz
    CompilerConfiguration compilerConfiguration

    def setup() {
        oldLoader = Thread.currentThread().contextClassLoader
        def importCustomizer = new ImportCustomizer()
        importCustomizer.addStarImports("com.cloudbees.groovy.cps")

        compilerConfiguration = new CompilerConfiguration()
        compilerConfiguration.addCompilationCustomizers(importCustomizer)
        compilerConfiguration.addCompilationCustomizers(new CpsTransformer())
        compilerConfiguration.addCompilationCustomizers(new ASTTransformationCustomizer(TypeChecked, AstChecker.name))

        loader = new GroovyClassLoader(Thread.currentThread().getContextClassLoader(), compilerConfiguration)
        Thread.currentThread().contextClassLoader = loader
    }

    def cleanup() {
        Thread.currentThread().contextClassLoader = oldLoader
    }

    def createClass(String code) {
        clazz = loader.parseClass(code)
    }

    def "calling CPS from NonCPS is illegal"() {
        when:
        createClass '''
def inner() {
    return ""
}

@NonCPS
def outer() {
    println inner()
}
'''
        then:
        thrown(MultipleCompilationErrorsException)
    }

    def "calling CPS-Getter via property style from NonCPS is illegal"() {
        when:
        createClass '''
def getInner() {
    return ""
}

@NonCPS
def outer() {
    return inner
}
'''
        then:
        thrown(MultipleCompilationErrorsException)
    }

    def "calling NonCPS-Getter via property style from NonCPS is legal"() {
        when:
        createClass '''
@NonCPS
def getInner() {
    return ""
}

@NonCPS
def outer() {
    return inner
}
'''
        then:
        notThrown(MultipleCompilationErrorsException)
    }

    def "Methods overriding NonCps methods must also be NonCps"() {
        when:
        createClass '''
class Parent {
    @NonCPS
    def myMethod() {}
}
class child extends Parent {
    def myMethod() {}
}
'''
        then:
        thrown(MultipleCompilationErrorsException)
    }

}