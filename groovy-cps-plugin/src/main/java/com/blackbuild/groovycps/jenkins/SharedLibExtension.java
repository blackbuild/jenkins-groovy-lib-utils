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

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

public abstract class SharedLibExtension {

    public static final String DEFAULT_PLUGIN_VERSIONS = "plugins/versions.properties";
    public static final String DEFAULT_PLUGIN_MAPPINGS = "plugins/mapping.properties";
    Project project;

    public SharedLibExtension(Project project) {
        this.project = project;
        getJenkinsVersion().convention("2.375.1");
        getPluginVersionsFile().convention(project.getLayout().getProjectDirectory().file(DEFAULT_PLUGIN_VERSIONS));
        getPluginMappingFile().convention(project.getLayout().getProjectDirectory().file(DEFAULT_PLUGIN_MAPPINGS));
    }

    public abstract Property<String> getJenkinsVersion();

    public abstract ListProperty<String> getPlugins();

    public void plugin(String plugin) {
        getPlugins().add(plugin);
    }

    public void plugin(Provider<String> plugin) {
        getPlugins().add(plugin);
    }

    public abstract RegularFileProperty getPluginVersionsFile();
    public abstract RegularFileProperty getPluginMappingFile();
}
