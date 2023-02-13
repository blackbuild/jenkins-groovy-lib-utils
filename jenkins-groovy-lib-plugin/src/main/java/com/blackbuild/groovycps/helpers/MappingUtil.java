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
package com.blackbuild.groovycps.helpers;

import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.io.Reader;
import java.util.Map;
import java.util.Properties;
import java.util.function.Function;

import static java.util.stream.Collectors.toMap;

/**
 * Handles property file creation and reading for plugin mapping and plugin versions.
 *
 */
public class MappingUtil {

    private MappingUtil() {}

    public static Map<String, String> loadPropertiesFromFile(File file) throws IOException {
        return loadPropertiesFromFile(file, Map.Entry::getKey, Map.Entry::getValue);
    }

    public static Map<String, String> loadPropertiesFromFile(File file, Function<Map.Entry<Object, Object>, Object> keyMapper, Function<Map.Entry<Object, Object>, Object> valueMapper) throws IOException {
        try (FileReader reader = new FileReader(file)) {
            return loadProperties(reader, keyMapper, valueMapper);
        }
    }

    public static Map<String, String> loadProperties(Reader reader, Function<Map.Entry<Object, Object>, Object> keyMapper, Function<Map.Entry<Object, Object>, Object> valueMapper) throws IOException {
        Properties properties = new Properties();
        properties.load(reader);
        return properties.entrySet().stream().collect(
                toMap(
                        keyMapper.andThen(String::valueOf),
                        valueMapper.andThen(String::valueOf)
                ));
    }

    public static Function<Map.Entry<Object, Object>, Object> mapToProperties(Function<Map.Entry<Object, Object>, Object> mapper, Map<String, String> map) {
        //noinspection SuspiciousMethodCalls
        return mapper.andThen(map::get);
    }
}
