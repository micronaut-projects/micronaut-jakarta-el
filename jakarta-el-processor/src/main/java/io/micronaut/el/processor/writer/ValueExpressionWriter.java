/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.el.processor.writer;

import io.micronaut.core.annotation.Generated;
import io.micronaut.core.annotation.Internal;
import io.micronaut.el.processor.compiler.ELCompiler;
import io.micronaut.el.processor.compiler.ELExpressionDefinition;
import io.micronaut.el.parser.ELNodes;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.CompiledValueExpression;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.el.ELContext;
import jakarta.el.ValueReference;

import javax.lang.model.element.Modifier;

/**
 * The writer of the {@code jakarta.el.ValueExpression} implementations.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ValueExpressionWriter {

    private static final ClassTypeDef EL_CONTEXT = ClassTypeDef.of(ELContext.class);
    private static final ClassTypeDef EL_RESOLUTION = ClassTypeDef.of(ELResolution.class);
    private static final TypeDef CLASS_TYPE = TypeDef.parameterized(ClassTypeDef.of(Class.class), TypeDef.wildcard());
    private static final String CONTEXT = "context";

    private ValueExpressionWriter() {
    }

    /**
     * Writes the implementation of a compiled value expression.
     *
     * @param className  The name of the generated class
     * @param definition The declared expression
     * @param compiler   The compiler
     * @return The definition of the generated class
     */
    public static ClassDef write(String className,
                                 ELExpressionDefinition definition,
                                 ELCompiler compiler) {
        ClassDef.ClassDefBuilder builder = ClassDef.builder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .superclass(ClassTypeDef.of(CompiledValueExpression.class))
            // the source writer interprets a '$' in the javadoc
            .addJavadoc("The compiled form of the expression <code>" + definition.expression().replace("$", "$$") + "</code>.")
            .addMethod(constructor(definition))
            .addMethod(evaluate(definition, compiler));

        ELNode node = unwrap(definition.node());
        if (isLValue(node)) {
            builder.addMethod(setValue(node, compiler));
            builder.addMethod(isReadOnly(node, compiler));
            builder.addMethod(getType(node, compiler));
            builder.addMethod(getValueReference(node, compiler));
        }
        return builder.build();
    }

    private static MethodDef constructor(ELExpressionDefinition definition) {
        return MethodDef.constructor()
            .addModifiers(Modifier.PUBLIC)
            .build((aThis, parameters) -> aThis.superRef().invokeSuperConstructor(
                ExpressionDef.constant(definition.expression()),
                ExpressionDef.constant(ELNodes.canonical(definition.node())),
                ExpressionDef.constant(TypeDef.erasure(definition.expectedType()))
            ));
    }

    private static MethodDef evaluate(ELExpressionDefinition definition, ELCompiler compiler) {
        return MethodDef.builder("evaluate")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) ->
                compiler.compile(definition.node(), parameters.get(0)).returning());
    }

    private static MethodDef setValue(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("setValue")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .addParameter("value", TypeDef.OBJECT)
            .returns(TypeDef.VOID)
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ELCompiler.LValue lValue = lValueOf(compiler, node, context);
                if (lValue.base() == null) {
                    // a void invocation is a statement, and is never wrapped in a cast
                    return (StatementDef) compiler.invokeRuntime(EL_RESOLUTION, "setIdentifier", TypeDef.VOID,
                        context, lValue.property(), parameters.get(1));
                }
                return (StatementDef) compiler.invokeRuntime(EL_RESOLUTION, "setValue", TypeDef.VOID,
                    context, lValue.base(), lValue.property(), parameters.get(1));
            });
    }

    private static MethodDef isReadOnly(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("isReadOnly")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.Primitive.BOOLEAN)
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ELCompiler.LValue lValue = lValueOf(compiler, node, context);
                if (lValue.base() == null) {
                    return compiler.invokeRuntime(EL_RESOLUTION, "isIdentifierReadOnly", TypeDef.Primitive.BOOLEAN,
                        context, lValue.property()).returning();
                }
                return compiler.invokeRuntime(EL_RESOLUTION, "isReadOnly", TypeDef.Primitive.BOOLEAN,
                    context, lValue.base(), lValue.property()).returning();
            });
    }

    private static MethodDef getType(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("getType")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(CLASS_TYPE)
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ELCompiler.LValue lValue = lValueOf(compiler, node, context);
                if (lValue.base() == null) {
                    return compiler.invokeRuntime(EL_RESOLUTION, "getIdentifierType", CLASS_TYPE,
                        context, lValue.property()).returning();
                }
                return compiler.invokeRuntime(EL_RESOLUTION, "getType", CLASS_TYPE,
                    context, lValue.base(), lValue.property()).returning();
            });
    }

    private static MethodDef getValueReference(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("getValueReference")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(ClassTypeDef.of(ValueReference.class))
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ELCompiler.LValue lValue = lValueOf(compiler, node, context);
                ExpressionDef base = lValue.base() == null ? ExpressionDef.nullValue() : lValue.base();
                return ClassTypeDef.of(ValueReference.class)
                    .instantiate(base, lValue.property())
                    .returning();
            });
    }

    private static ELCompiler.LValue lValueOf(ELCompiler compiler, ELNode node, ExpressionDef context) {
        ELCompiler.LValue lValue = compiler.compileLValue(node, context);
        if (lValue == null) {
            throw new IllegalStateException("The expression is not an lvalue: " + node);
        }
        return lValue;
    }

    private static ELNode unwrap(ELNode node) {
        return node instanceof ELNode.Eval eval ? eval.expression() : node;
    }

    private static boolean isLValue(ELNode node) {
        return node instanceof ELNode.Identifier || node instanceof ELNode.Property;
    }
}
