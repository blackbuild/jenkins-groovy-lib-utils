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

import com.cloudbees.groovy.cps.NonCPS;
import com.cloudbees.groovy.cps.WorkflowTransformed;
import org.codehaus.groovy.ast.ASTNode;
import org.codehaus.groovy.ast.ClassHelper;
import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.ast.MethodNode;
import org.codehaus.groovy.ast.expr.Expression;
import org.codehaus.groovy.ast.expr.MethodCall;
import org.codehaus.groovy.transform.stc.AbstractTypeCheckingExtension;

import static org.codehaus.groovy.runtime.DefaultGroovyMethods.asType;

/**
 * {@link org.codehaus.groovy.transform.stc.TypeCheckingExtension} that checks for calls to CPS transformed methods
 * from within a NonCPS annotated method
 */
public class CpsCheckExtension extends AbstractTypeCheckingExtension {
    CpsCheckExtension(CpsCheckVisitor typeCheckingVisitor) {
        super(typeCheckingVisitor);
        this.cpsCheckVisitor = typeCheckingVisitor;
    }

    @Override
    public void afterMethodCall(MethodCall call) {
        final MethodNode method = typeCheckingVisitor.getTypeCheckingContext().getEnclosingMethod();
        if (method == null) return;
        if (isCpsTransformed(method)) return;

        MethodNode targetMethod = getTargetMethod(asType(call, Expression.class));
        if (targetMethod == null) return;
        if (isCpsTransformed(targetMethod)) {

            cpsCheckVisitor.addCPSTypeError(
                    "Illegal call from NonCPS method " + method.getName(),
                    asType(call, ASTNode.class));
        }

    }

    private static boolean isCpsTransformed(MethodNode method) {
        return !method.getAnnotations(WORKFLOW_TRANSFORMED_ANNOTATION).isEmpty();
    }

    static final ClassNode NON_CPS_ANNOTATION_TYPE = ClassHelper.make(NonCPS.class);
    static final ClassNode WORKFLOW_TRANSFORMED_ANNOTATION = ClassHelper.make(WorkflowTransformed.class);
    private final CpsCheckVisitor cpsCheckVisitor;
}
