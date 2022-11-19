configuration
        .addCompilationCustomizers(new org.codehaus.groovy.control.customizers.ImportCustomizer()
        .addStarImports("com.cloudbees.groovy.cps", "hudson.model", "jenkins.model"))
        .addCompilationCustomizers(new com.cloudbees.groovy.cps.CpsTransformer()) // use CPS Transformation
        .addCompilationCustomizers(new org.codehaus.groovy.control.customizers.ASTTransformationCustomizer(groovy.transform.TypeChecked, "com.blackbuild.groovy.cps.astchecker.AstChecker")) // check for CPS violations
        .setScriptBaseClass("org.jenkinsci.plugins.workflow.cps.CpsScript") // for Jenkinsfiles

