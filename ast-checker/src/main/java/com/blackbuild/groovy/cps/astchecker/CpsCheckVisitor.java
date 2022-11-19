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
package com.blackbuild.groovy.cps.astchecker;

import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.AnnotatedNode;
import org.codehaus.groovy.ast.ClassCodeVisitorSupport;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.PropertyExpression;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

import static java.util.Arrays.stream;
import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asType;

/**
 * {@link StaticTypeCheckingVisitor} that include various CPS specific validations in the compile process:
 * <ul>
 *     <li>Methods that override NonCPS methods must be marked with NonCPS themselves</li>
 *     <li>CPS methods must not use default values for arguments</li>
 *     <li>NonCPS methods must not call CPS methods (via {@link CpsCheckExtension}</li>
 *     <li>NonCPS methods must not call CPS getters (using property style access)</li>
 * </ul>
 */
public class CpsCheckVisitor extends StaticTypeCheckingVisitor {
    public CpsCheckVisitor(SourceUnit source, ClassNode cn) {
        super(source, cn);
        addTypeCheckingExtension(new CpsCheckExtension(this));
    }

    @Override
    public boolean isSkipMode(AnnotatedNode node) {
        // skip all CPS transformed methods
        if (node instanceof MethodNode && !isNonCps((MethodNode) node)) return true;

        return super.isSkipMode(node);
    }

    @Override
    protected boolean existsProperty(final PropertyExpression pexp, final boolean checkForReadOnly) {
        return existsProperty(pexp, checkForReadOnly, new FailIfGetterIsCPSMethodVisitor(this));
    }

    @Override
    protected void addStaticTypeError(String msg, ASTNode expr) {
        // ignore, we only use static type to determine the call targets
    }

    public void addCPSTypeError(String message, ASTNode astNode) {
        super.addStaticTypeError(message, astNode);
    }

    @Override
    public void visitMethod(MethodNode node) {
        super.visitMethod(node);

        assertNonCpsOverridingIsConsistent(node);
        if (isNonCps(node)) return;
        assertNoInitialExpressionInCpsMethods(node);
    }

    private void assertNoInitialExpressionInCpsMethods(MethodNode node) {
        stream(node.getParameters())
                .filter(param -> param.getInitialExpression() != null)
                .forEach(param -> addCPSTypeError("Default values are not allowed for CPS methods", param));
    }

    private void assertNonCpsOverridingIsConsistent(MethodNode node) {
        MethodNode overriddenMethod = node.getDeclaringClass().getSuperClass().getDeclaredMethod(node.getName(), node.getParameters());

        if (overriddenMethod != null && isNonCps(overriddenMethod) && !isNonCps(node))
            addCPSTypeError("Methods overriding NonCPS methods must be non-CPS themselves", node);
    }

    static boolean isNonCps(MethodNode node) {
        return !node.getAnnotations(CpsCheckExtension.NON_CPS_ANNOTATION_TYPE).isEmpty();
    }

    public static class FailIfGetterIsCPSMethodVisitor extends ClassCodeVisitorSupport {
        private final CpsCheckVisitor outer;

        public FailIfGetterIsCPSMethodVisitor(CpsCheckVisitor outer) {
            this.outer = outer;
        }

        @Override
        protected SourceUnit getSourceUnit() {
            return outer.getSourceUnit();
        }

        @Override
        public void visitMethod(MethodNode node) {
            if (isNonCps(node)) return;
            outer.addCPSTypeError(String.format(
                    "Illegal call from NonCPS method %s to CPS getter %s::%s",
                            getEnclosingMethodName(),
                            node.getDeclaringClass().getName(), node.getName()
                    ),
                    asType(outer.getTypeCheckingContext().getEnclosingMethod(), ASTNode.class));
        }

        private String getEnclosingMethodName() {
            return outer.getTypeCheckingContext().getEnclosingClassNode().getName() + "::" + outer.getTypeCheckingContext().getEnclosingMethod().getName();
        }

    }
}
