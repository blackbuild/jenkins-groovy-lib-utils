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

import org.codehaus.groovy.ast.ClassNode;
import org.codehaus.groovy.control.CompilePhase;
import org.codehaus.groovy.control.SourceUnit;
import org.codehaus.groovy.transform.GroovyASTTransformation;
import org.codehaus.groovy.transform.StaticTypesTransformation;
import org.codehaus.groovy.transform.stc.StaticTypeCheckingVisitor;

/**
 * StaticTypeChecker for CPS Transformations. The only difference to the default
 * Transformation is the usage of a different Visitor ({@link CpsCheckVisitor}
 * instead of {@link StaticTypeCheckingVisitor}.
 * <p>
 * Can be used in one of two ways:
 * <ul>
 *     <li>By including a compile customizer in the groovy compile process:
 *     <code>new ASTTransformationCustomizer(TypeChecked, "com.blackbuild.groovy.cps.astchecker.AstChecker"))</code></li>
 *     <li>for gradle builds by using the <code>groovy-cps-plugin</code></li>
 * </ul>
 * Note that explicitly declaring the {@link groovy.transform.TypeChecked} annotation on a class would work, but such
 * a class could not be used in a Jenkins Pipeline.
 */
@GroovyASTTransformation(phase = CompilePhase.INSTRUCTION_SELECTION)
public class AstChecker extends StaticTypesTransformation {

    @Override
    protected StaticTypeCheckingVisitor newVisitor(SourceUnit unit, ClassNode node) {
        return new CpsCheckVisitor(unit, node);
    }

}
