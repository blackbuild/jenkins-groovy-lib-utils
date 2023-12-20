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

import org.gradle.api.Project;
import org.gradle.api.file.RegularFileProperty;
import org.gradle.api.provider.ListProperty;
import org.gradle.api.provider.Property;
import org.gradle.api.provider.Provider;

@SuppressWarnings("unused")
public abstract class JenkinsSharedLibExtension {

    public static final String DEFAULT_PLUGIN_VERSIONS = "plugins/versions.properties";
    public static final String DEFAULT_PLUGIN_MAPPINGS = "plugins/mapping.properties";
    public static final String DEFAULT_JENKINS_UPDATE_CENTER = "https://updates.jenkins.io/current/update-center.json";

    public JenkinsSharedLibExtension(Project project) {
        getAddTestBaseDependency().convention(true);
    }

    public abstract Property<Boolean> getAddTestBaseDependency();

    public void doNotAddTestBaseDependency() {
        getAddTestBaseDependency().set(false);
    }

}
