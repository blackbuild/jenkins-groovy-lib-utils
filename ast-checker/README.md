AST-Checker
===========
Einfaches System um ein paar Syntax Checks gegen den AST (abstract syntax tree) auszuführen

Der AST ist eine Vorstufe des Compilers, der Parser zerlegt hier die Source-Code in Symbole. Auf diesen Baum
von Symbolen können Check effizienter angewandt werden, als direkt auf dem Source.Code.

Die Details dir wir testen sind aus der Testklasse ersichtlich.

Die Verwendung erfolgt mehrstufig:

über die Compiler-Customization (.groovyCompile.groovy im Hauptprojekt) wir mit dieser Zeile:

```groovy
    configuration.addCompilationCustomizers(new ASTTransformationCustomizer(TypeChecked, "com.blackbuild.groovy.cps.astchecker.AstChecker")) // check for CPS violations
```

implizit jeder Klasse die Annotation `@TypeChecked("com.blackbuild.groovy.cps.astchecker.AstChecker")` hinzugefügt (siehe https://docs.groovy-lang.org/2.4.21/html/gapi/groovy/transform/TypeChecked.html)

Wir verwenden hier nicht nur eine Extension, sondern einen eigenen Checker, weil wir auch den zugrunde liegenden Visitor überschreiben müssen.

Der AstChecker selbst bindet dann auch unsere eigene Extension ein.

# AstChecker
Der AstChecker ist eine StaticTypesTransformation, der einzige Unterschied ist der andere Visitor.

# CpsCheckVisitor

Erfüllt mehrere Aufgaben:

- Bindet die CpsCheckExtension mit ein
- ignoriert alle CpsTransformierte Methoden (wir machen nicht wirklich static type checking), also alle Methoden _ohne_ @NonCPS
- ignoriert alle Statischen Type Fehler, die über den regulären Mechanismus hereinkommen
- nur Fehler, die über die eigene Methode hereinkommen, werden gemeldet und führen zu Compile-Fehlern (das ist etwas dreckig, aber da Groovy 2.4 
  nicht mehr weiter entwickelt wird, ist das safe. Falls Jenkins mal auf Groovy 3 updated, muss hier eh neu ran-gegangen werden).
- überprüft den Zugriff von NonCps Methode auf CPS-Getter im Property-Style Syntax (also bla.name für getName()), das kann die Extension nicht abfangen

# CpsCheckExtension

Überprüft für jede NonCPS Method (alle anderen werden ja durch den Visitor schon übersprungen), ob diese Methode aufruft, die transformiert wurden
(transformierte Methoden werden mit einer Annotation versehen)

