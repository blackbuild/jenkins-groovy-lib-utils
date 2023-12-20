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
package com.blackbuild.groovycps.sharedlib;

import com.blackbuild.groovycps.helpers.PluginHelper;
import com.blackbuild.groovycps.jenkins.JenkinsDependenciesPlugin;
import com.blackbuild.groovycps.plugin.GroovyCpsPlugin;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.*;
import org.gradle.api.tasks.GroovySourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.HashMap;
import java.util.Map;

import static com.blackbuild.groovycps.helpers.MappingUtil.loadPropertiesFromFile;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;

@SuppressWarnings("unused")
public class JenkinsSharedLibPlugin implements Plugin<Project> {

    public static final String DEFAULT_JENKINS_REPO = "https://repo.jenkins-ci.org/public/";
    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Project project;
    private JenkinsSharedLibExtension extension;
    private Configuration jenkinsCore;
    private Configuration jenkinsPlugins;
    private Map<String, String> pluginMapping;

    private Map<String, String> pluginVersions;
    private final Map<String, String> explicitPluginVersions = new HashMap<>();

    private static void configureConfiguration(Configuration c) {
        c.setVisible(false);
        c.setCanBeConsumed(false);
        c.setCanBeResolved(true);
    }

    @Override
    public void apply(Project project) {
        this.project = project;
        project.getPluginManager().apply(JenkinsDependenciesPlugin.class);
        project.getPluginManager().apply(GroovyCpsPlugin.class);
        extension = project.getExtensions().create("sharedLib", JenkinsSharedLibExtension.class, project);

        addTestBase();
        configureSourceSets();
    }

    private void addTestBase() {
        if (extension.getAddTestBaseDependency().get())
            project.getDependencies().add(
                    "testImplementation",
                    project.getDependencies().create("com.blackbuild.groovycps:jenkins-test-base:" + PluginHelper.getOwnVersion()));
    }


    @SuppressWarnings({"UnstableApiUsage", "DataFlowIssue"})
    private void configureSourceSets() {
        SourceSetContainer sourceSets = project.getExtensions().getByType(SourceSetContainer.class);
        SourceSet main = sourceSets.getByName(SourceSet.MAIN_SOURCE_SET_NAME);
        main.getExtensions().findByType(GroovySourceDirectorySet.class).setSrcDirs(asList("src", "vars", "jenkins"));
        main.getResources().setSrcDirs(asList("gdsl", "resources"));

        SourceSet test = sourceSets.getByName(SourceSet.TEST_SOURCE_SET_NAME);
        test.getExtensions().findByType(GroovySourceDirectorySet.class).setSrcDirs(singletonList("test"));
        test.getResources().setSrcDirs(singletonList("testResources"));
    }
}
