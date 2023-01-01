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
package com.blackbuild.groovycps.plugin;

import com.blackbuild.groovy.cps.astchecker.AstChecker;
import com.cloudbees.groovy.cps.NonCPS;
import org.gradle.api.Action;
import org.gradle.api.GradleException;
import org.gradle.api.Plugin;
import org.gradle.api.Project;
import org.gradle.api.Task;
import org.gradle.api.artifacts.Configuration;
import org.gradle.api.plugins.GroovyPlugin;
import org.gradle.api.resources.TextResource;
import org.gradle.api.tasks.compile.GroovyCompile;

import java.io.File;
import java.net.URL;
import java.security.CodeSource;

/**
 * Plugin that prepares a project for CPS usage. Applies the "groovy" plugin
 * and prepares main compilation to apply GroovyCPS to all classes.
 */
public class GroovyCpsPlugin implements Plugin<Project> {

    private static final String GROOVY_CONFIGURATION = "groovy";
    private Project project;

    @Override
    public void apply(Project project) {
        this.project = project;
        project.getPluginManager().apply(GroovyPlugin.class);

        createGroovyConfiguration();
        activateCps();
    }

    private void createGroovyConfiguration() {
        Configuration groovy = project.getConfigurations().create(GROOVY_CONFIGURATION);
        project.getConfigurations().getByName("implementation").extendsFrom(groovy);
        addCpsDependenciesTo(groovy);
    }

    private void addCpsDependenciesTo(Configuration groovy) {
        // TODO: Better include the actual Plugin dependencies
        groovy.getDependencies().add(project.getDependencies().create(
                project.files(getLocationForClass(AstChecker.class), getLocationForClass(NonCPS.class))
        ));
    }

    private static URL getLocationForClass(Class<?> type) {
        CodeSource source = type.getProtectionDomain().getCodeSource();
        if (source == null)
            throw new GradleException("Could not determine classpath of " + type);
        return source.getLocation();
    }

    private void activateCps() {
        GroovyCompile compileGroovy = (GroovyCompile) project.getTasks().getByName("compileGroovy");
        // compileGroovy.doFirst(new SetClassPathAction());
        compileGroovy.setGroovyClasspath(project.getConfigurations().getByName(GROOVY_CONFIGURATION));

        File existingConfigScript = compileGroovy.getGroovyOptions().getConfigurationScript();
        if (existingConfigScript != null)
            throw new GradleException("Groovy-CPS-Plugin does not support existing configuration scripts yet.");

        TextResource scriptText = project.getResources().getText().fromUri(this.getClass().getResource(".groovyCompile.groovy"));
        compileGroovy.getGroovyOptions().setConfigurationScript(scriptText.asFile());
    }

    private static class SetClassPathAction implements Action<Task> {
        @Override
        public void execute(Task task) {
            GroovyCompile groovyCompile = (GroovyCompile) task;
            groovyCompile.setGroovyClasspath(task.getProject().getConfigurations().getByName(GROOVY_CONFIGURATION));
        }
    }
}
