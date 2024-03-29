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

import com.blackbuild.groovycps.helpers.MappingUtil
import spock.lang.Specification

class MappingUtilTest extends Specification {

    def "normal properties are read"() {
        given:
        // language=Properties
        def content = '''
bla=blub
bli=bleu
'''
        when:
        def result = MappingUtil.loadProperties(new StringReader(content), Map.Entry::getKey, Map.Entry::getValue)

        then:
        result == [bla: 'blub', bli: 'bleu']
    }

    def "properties are read with converter"() {
        given:
        // language=Properties
        def content = '''
bla=blub
bli=bleu
'''
        def map = [bla: 'bla:123', bli: 'blibli:blu']

        when:
        def result = MappingUtil.loadProperties(new StringReader(content), MappingUtil.mapToProperties(Map.Entry::getKey, map), Map.Entry::getValue)

        then:
        result == ['bla:123': 'blub', 'blibli:blu': 'bleu']
    }

}
