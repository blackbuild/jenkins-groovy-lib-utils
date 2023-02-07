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

import java.io.BufferedInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Map;
import java.util.Properties;

public abstract class UpdatePluginVersions extends DefaultTask {

    @OutputFile
    protected abstract RegularFileProperty getPluginVersionsFile();

    @Input
    protected abstract Property<String> getInstalledPluginsUrl();

    @SuppressWarnings("unchecked")
    @TaskAction
    public void readPluginMapping() throws IOException {
        URL url;
        try {
            url = getProject().uri(getInstalledPluginsUrl().get()).toURL();
        } catch (MalformedURLException e) {
            throw new GradleException("Could not create url to installed.json", e);
        }
        Properties versions = new Properties();
        try (InputStream in = new BufferedInputStream(url.openStream())) {
            Map<String, Object> parse = (Map<String, Object>) new JsonSlurper().parse(in);
            List<Map<String, Object>> plugins = (List<Map<String, Object>>) parse.get("plugins");

            plugins.forEach(v -> versions.setProperty(v.get("shortName").toString(), v.get("version").toString()));
        }
        PropertiesUtils.store(versions, getPluginVersionsFile().getAsFile().get());
    }
}
