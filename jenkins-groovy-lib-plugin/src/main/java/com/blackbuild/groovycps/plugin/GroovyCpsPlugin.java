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
package com.blackbuild.groovycps.plugin;

import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.artifacts.Dependency;
import org.gradle.api.internal.provider.DefaultProvider;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.provider.Provider;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.compile.GroovyCompile;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.util.Properties;

/**
 * Plugin that prepares a project for CPS usage. Applies the "groovy" plugin
 * and prepares main compilation to apply GroovyCPS to all classes.
 */
public class GroovyCpsPlugin implements Plugin<Project> {

    private static final String GROOVY_CONFIGURATION = "groovy";
    private Project project;
    private GroovyCpsPluginExtension extension;

    @Override
    public void apply(Project project) {
        this.project = project;
        project.getPluginManager().apply(GroovyPlugin.class);

        extension = project.getExtensions().create("cps", GroovyCpsPluginExtension.class);
        extension.getCpsVersion().convention("1.32");
        extension.getGroovyVersion().convention("2.4.21");

        createGroovyConfiguration();
        activateCps();
    }

    private void createGroovyConfiguration() {
        Configuration groovy = project.getConfigurations().create(GROOVY_CONFIGURATION, c -> {
            c.setVisible(false);
            c.setCanBeConsumed(false);
            c.setCanBeResolved(true);
            c.setDescription("Dependencies for the groovy compiler");
        });
        project.getConfigurations().getByName("implementation").extendsFrom(groovy);
        addCpsDependenciesTo(groovy);
    }

    private void addCpsDependenciesTo(Configuration groovy) {
        groovy.defaultDependencies(d -> {
            d.add(createDependency("com.blackbuild.groovycps:ast-checker", new DefaultProvider<>(this::getOwnVersion)));
            d.add(createDependency("org.codehaus.groovy:groovy-all", extension.getGroovyVersion()));
            d.add(createDependency("com.cloudbees:groovy-cps", extension.getCpsVersion()));
        });
    }

    private String getOwnVersion() {
        Properties props = new Properties();
        try (InputStream is = getClass().getClassLoader().getResourceAsStream("com.blackbuild.jenkins.groovy-cps.properties")) {
            props.load(is);
        } catch (IOException e) {
            throw new GradleException("Could not determine version of groovy cps plugin.",e);
        }
        return props.getProperty("version");
    }

    private Dependency createDependency(String artifactAndGroup, Provider<String> version) {
        return project.getDependencies().create(artifactAndGroup + ":" + version.get());
    }

    private void activateCps() {
        GroovyCompile compileGroovy = (GroovyCompile) project.getTasks().getByName("compileGroovy");
        compileGroovy.setGroovyClasspath(project.getConfigurations().getByName(GROOVY_CONFIGURATION));

        File existingConfigScript = compileGroovy.getGroovyOptions().getConfigurationScript();
        if (existingConfigScript != null)
            throw new GradleException("Groovy-CPS-Plugin does not support existing configuration scripts yet.");

        TextResource scriptText = project.getResources().getText().fromUri(this.getClass().getResource(".groovyCompile.groovy"));
        compileGroovy.getGroovyOptions().setConfigurationScript(scriptText.asFile());
    }


}
