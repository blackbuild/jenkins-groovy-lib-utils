Unit Test for Jenkins Groovy Libraries
======================================

This project allows easy testing of a Jenkins Groovy Library (formerly called Jenkins Shared Lib).

It consists of three elements:

- The AST checker provides compile-time checks for various hard to find code problems
- The Jenkins Groovy Lib Plugin provides a simple way to test Pipeline Libraries
- Jenkins Test Base contains a useful classes to simplify testing

## Usage

```groovy
plugins {
    id "com.blackbuild.jenkins.shared-lib" version "0.1.0-rc.1"
}

// Java 1.8 prevents ugly warnings in build
sourceCompatibility = JavaVersion.VERSION_11
targetCompatibility = JavaVersion.VERSION_11

jenkins {
    jenkinsVersion = '2.319.1'
    installedPluginsUrl = "file:plugins/installed.json"
    plugin "lockable-resources" // shortname without version
    plugin "workflow-cps:2648.va9433432b33c" // shortname and explicit version
    plugin 'pipeline-utility-steps'
    plugin "org.jenkins-ci.plugins:http_request" //
    plugin 'pipeline-milestone-step'
}

repositories {
    maven {
        url "https://repo.jenkins-ci.org/public/"
    }
}

dependencies {
    testImplementation "com.blackbuild.groovycps:jenkins-test-base:0.1.0-rc.1"
}
```
## Plugin Mappings
In order to correctly map the plugins, two files can be provided:

### mapping.properties

Provides a mapping "plugin-shortname" to GroupId-ArtifactId. This can be created from a Jenkins Update-Center file using the provided task "UpdatePluginMapping", which defaults to the standard update center files.

If this file is missing, shortnames cannot be used.

### version.properties

Provides versions for plugins. This can be used to use the exact plugin set from
an existing Jenkins installation.

The url for the update task should point to "https://<jenkins>/pluginManager/api/json?depth=1", however, since this url is usually protected, the easiest way is to manually copy this file into the plugin folder and use a file url to access it (or use something like the download-plugin to do it). 



TBC

# Example project

https://github.com/blackbuild/jenkins-groovy-lib-utils-example
