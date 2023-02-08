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
package com.blackbuild.groovycps.jenkins;

import groovy.json.JsonSlurper;
import org.gradle.api.DefaultTask;
import org.gradle.api.GradleException;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.tasks.Input;
import org.gradle.api.tasks.OutputFile;
import org.gradle.api.tasks.TaskAction;
import org.gradle.internal.util.PropertiesUtils;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.Map;
import java.util.Properties;

public abstract class UpdatePluginMappings extends DefaultTask {

    @OutputFile
    protected abstract RegularFileProperty getPluginMappingFile();

    @Input
    protected abstract Property<String> getUpdateCenterUrl();

    @SuppressWarnings("unchecked")
    @TaskAction
    public void readPluginMapping() throws IOException {
        URL url;
        try {
            url = getProject().uri(getUpdateCenterUrl().get()).toURL();
        } catch (MalformedURLException e) {
            throw new GradleException("Could not create url to update center", e);
        }
        Properties pluginMap = new Properties();
        try (InputStream in = url.openStream()) {
            StripJsonpReader jsonpReader = new StripJsonpReader(new InputStreamReader(in));
            Map<String, Object> parse = (Map<String, Object>) new JsonSlurper().parse(jsonpReader);
            Map<String, Map<String, Object>> plugins = (Map<String, Map<String, Object>>) parse.get("plugins");
            plugins.forEach((k, v) -> {
                String[] gav = v.get("gav").toString().split(":");
                pluginMap.setProperty(k, gav[0] + ":" + gav[1]);
            });
        }
        PropertiesUtils.store(pluginMap, getPluginMappingFile().getAsFile().get());
    }
}
