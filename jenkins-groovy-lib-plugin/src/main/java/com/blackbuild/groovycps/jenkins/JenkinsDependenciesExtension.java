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

import org.gradle.api.Project;
import org.gradle.api.file.DirectoryProperty;
import org.gradle.api.file.RegularFile;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

@SuppressWarnings("unused")
public abstract class JenkinsDependenciesExtension {

    public static final String DEFAULT_PLUGIN_VERSIONS = "plugins/versions.properties";
    public static final String DEFAULT_PLUGIN_MAPPINGS = "plugins/mapping.properties";
    public static final String DEFAULT_JENKINS_UPDATE_CENTER = "https://updates.jenkins.io/current/update-center.json";
    public static final String DEFAULT_JENKINS_VERSION = "2.375.1";
    public static final String DEFAULT_TEST_HARNESS_VERSION = "2129.v09f309d2339c";

    @SuppressWarnings("java:S5993")
    public JenkinsDependenciesExtension(Project project) {
        getJenkinsVersion().convention(DEFAULT_JENKINS_VERSION);
        getJenkinsTestHarnessVersion().convention(DEFAULT_TEST_HARNESS_VERSION);
        getPluginVersionsFile().convention(project.getLayout().getProjectDirectory().file(DEFAULT_PLUGIN_VERSIONS));
        getPluginMappingFile().convention(project.getLayout().getProjectDirectory().file(DEFAULT_PLUGIN_MAPPINGS));
        getUpdateCenterUrl().convention(DEFAULT_JENKINS_UPDATE_CENTER);
        getAddJenkinsRepository().convention(true);
        getUseTestHarness().convention(false);
        getPluginDirectory().convention(project.getLayout().getBuildDirectory().dir("jenkins-plugins/test-dependencies"));
    }

    public abstract Property<String> getJenkinsVersion();
    public abstract Property<String> getJenkinsTestHarnessVersion();

    public abstract ListProperty<String> getPlugins();

    public void plugin(String plugin) {
        getPlugins().add(plugin);
    }

    public void plugin(Provider<String> plugin) {
        getPlugins().add(plugin);
    }

    public abstract RegularFileProperty getPluginVersionsFile();
    public abstract RegularFileProperty getPluginMappingFile();

    public abstract Property<String> getInstalledPluginsUrl();

    public abstract Property<String> getUpdateCenterUrl();

    public abstract Property<Boolean> getAddJenkinsRepository();

    public abstract Property<Boolean> getUseTestHarness();

    public void useTestHarness() {
        getUseTestHarness().set(true);
    }

    public void doNotAddJenkinsRepository() {
        getAddJenkinsRepository().set(false);
    }

    public abstract DirectoryProperty getPluginDirectory();

    public Provider<RegularFile> getIndexFile() {
        return getPluginDirectory().file("index");
    }
}
