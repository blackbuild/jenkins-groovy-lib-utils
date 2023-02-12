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

import com.blackbuild.groovycps.plugin.GroovyCpsPlugin;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.artifacts.DependencyResolveDetails;
import org.gradle.api.artifacts.DependencySet;
import org.gradle.api.artifacts.ExternalDependency;
import org.gradle.api.artifacts.ModuleDependency;
import org.gradle.api.artifacts.ModuleVersionIdentifier;
import org.gradle.api.artifacts.ResolutionStrategy;
import org.gradle.api.artifacts.ResolvedArtifact;
import org.gradle.api.provider.Provider;
import org.gradle.api.tasks.GroovySourceDirectorySet;
import org.gradle.api.tasks.SourceSet;
import org.gradle.api.tasks.SourceSetContainer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.io.IOException;
import java.util.Collections;
import java.util.HashMap;
import java.util.Map;

import static com.blackbuild.groovycps.jenkins.MappingUtil.loadPropertiesFromFile;
import static com.blackbuild.groovycps.jenkins.MappingUtil.mapToProperties;
import static java.lang.String.format;
import static java.util.Arrays.asList;
import static java.util.Collections.singletonList;
import static java.util.Collections.singletonMap;

@SuppressWarnings("unused")
public class SharedLibPlugin implements Plugin<Project> {

    private final Logger logger = LoggerFactory.getLogger(getClass());

    private Project project;
    private SharedLibExtension extension;
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
        project.getPluginManager().apply(GroovyCpsPlugin.class);
        extension = project.getExtensions().create("jenkins", SharedLibExtension.class, project);

        configureSourceSets();
        createJenkinsConfigurations();
        addJenkinsCoreDependency();

        createHelperTasks();
    }

    private void createHelperTasks() {
        project.getTasks().register("updatePluginMappings", UpdatePluginMappings.class, t -> {
            t.setDescription("Updates the plugin mappings from the configured mapping URI");
            t.setGroup("help");
            t.getUpdateCenterUrl().set(extension.getUpdateCenterUrl());
            t.getPluginMappingFile().set(extension.getPluginMappingFile());
        });
        project.getTasks().register("updatePluginVersions", UpdatePluginVersions.class, t -> {
            t.setDescription("Updates the plugin versions from the configured mapping URI");
            t.setGroup("help");
            t.getInstalledPluginsUrl().set(extension.getInstalledPluginsUrl());
            t.getPluginVersionsFile().set(extension.getPluginVersionsFile());
        });
    }

    private void createJenkinsConfigurations() {
        jenkinsCore = project.getConfigurations().create("jenkinsCore", SharedLibPlugin::configureConfiguration);
        jenkinsPlugins = project.getConfigurations().create("jenkinsPlugins", SharedLibPlugin::configureConfiguration);
        jenkinsPlugins.extendsFrom(jenkinsCore);
        jenkinsPlugins.withDependencies(this::resolvePlugins);
        jenkinsPlugins.resolutionStrategy(this::resolvePluginVersions);
        project.getConfigurations().getByName("implementation").withDependencies(this::addPluginJarsToConfiguration);
        project.getConfigurations().all(config -> config.exclude(singletonMap("group", "commons-discovery")));
    }

    private void addPluginJarsToConfiguration(DependencySet dependencies) {
        jenkinsPlugins.getResolvedConfiguration().getResolvedArtifacts().forEach(plugin -> copyPluginToImplementation(dependencies, plugin));
    }

    private void copyPluginToImplementation(DependencySet dependencies, ResolvedArtifact dep) {
        ModuleVersionIdentifier moduleVersion = dep.getModuleVersion().getId();
        String dependencyString = format("%s:%s:%s", moduleVersion.getGroup(), moduleVersion.getName(), moduleVersion.getVersion());

        if ("hpi".equals(dep.getExtension()))
            dependencies.add(project.getDependencies().create(dependencyString + "@jar"));
        dependencies.add(project.getDependencies().create(dependencyString));
    }

    private void resolvePluginVersions(ResolutionStrategy resolutionStrategy) {
        resolutionStrategy.eachDependency(this::resolveSinglePluginVersion);
    }

    private void resolveSinglePluginVersion(DependencyResolveDetails details) {
        String ga = details.getRequested().getGroup() + ":" + details.getRequested().getName();
        String explicitVersion = explicitPluginVersions.get(ga);
        String defaultVersion = pluginVersions.get(ga);

        if (explicitVersion != null)
            details.because("Explicitly requested").useVersion(explicitVersion);
        else if (defaultVersion != null)
            details.because("From Plugin-Mapping").useVersion(defaultVersion);
    }

    private void resolvePlugins(DependencySet plugins) {
        loadPluginMappings();
        loadPluginVersions();
        addPluginsByShortName(plugins);
        plugins.forEach(this::resolveSinglePlugin);
    }

    private void addPluginsByShortName(DependencySet plugins) {
        extension.getPlugins().get().forEach(identifier -> addSinglePluginByShortName(plugins, identifier));
    }

    private void addSinglePluginByShortName(DependencySet plugins, String identifier) {
        logger.debug("Adding plugin {}", identifier);
        String[] elements = identifier.split(":", 3);

        String ga;
        String explicitVersion = null;
        if (elements.length == 1) {
            logger.debug("Single element, assuming short name");
            ga = pluginMapping.get(elements[0]);
            if (ga == null)
                throw new GradleException(format("Plugin %s not found in plugin mapping.", elements[0]));
        } else if (elements.length == 3) {
            logger.debug("Three elements, assuming full GAV");
            ga = String.format("%s:%s", elements[0], elements[1]);
            explicitVersion = elements[2];
        } else {
            logger.debug("Two elements, checking for shortName:version");
            ga = pluginMapping.get(elements[0]);
            if (ga == null) {
                logger.debug("no shortname found, assuming GA");
                ga = identifier;
            } else {
                explicitVersion = elements[1];
            }
        }

        logger.debug("{} is resolved to {}", identifier, ga);
        plugins.add(project.getDependencies().create(ga));

        if (explicitVersion != null)
            explicitPluginVersions.put(ga, explicitVersion);
    }

    private void loadPluginVersions() {
        if (pluginVersions != null)
            return;
        try {
            pluginVersions = loadPropertiesFromFile(
                    extension.getPluginVersionsFile().getAsFile().get(),
                    mapToProperties(Map.Entry::getKey, pluginMapping),
                    Map.Entry::getValue);
        } catch (IOException e) {
            logger.warn("Could not load plugin versions, explicit versions needed.");
            pluginVersions = Collections.emptyMap();
        }
    }

    private void loadPluginMappings() {
        if (pluginMapping != null)
            return;
        try {
            pluginMapping = loadPropertiesFromFile(extension.getPluginMappingFile().getAsFile().get());
        } catch (IOException e) {
            logger.warn("Could not load plugin mappings, plugin shortnames not supported.");
            pluginMapping = Collections.emptyMap();
        }
    }

    private void resolveSinglePlugin(Dependency dependency) {
        if (dependency.getVersion() != null || !(dependency instanceof ExternalDependency)) return;
        ((ExternalDependency) dependency).version(c ->
                c.require(pluginVersions.get(dependency.getGroup() + ":" + dependency.getName()))
        );
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

    private void addJenkinsCoreDependency() {
        jenkinsCore.defaultDependencies(d -> {
            Provider<String> coordinates = extension.getJenkinsVersion().map(it -> "org.jenkins-ci.main:jenkins-core:" + it);
            ModuleDependency jenkins = (ModuleDependency) project.getDependencies().create(coordinates.get());
            jenkins.exclude(singletonMap("module", "groovy"));
            jenkins.exclude(singletonMap("module", "groovy-all"));
            d.add(jenkins);
        });
    }
}
