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
import io.micronaut.el.processor.compiler.ELCompilationException;
import io.micronaut.el.processor.compiler.ELCompiler;
import io.micronaut.el.processor.compiler.ELExpressionDefinition;
import io.micronaut.el.parser.ELNodes;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.inject.ast.ClassElement;
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

import java.util.List;

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
    public static Written write(String className,
                                ELExpressionDefinition definition,
                                ELCompiler compiler) {
        // the evaluation is compiled first: an omitted expected type is inferred from its static type, and the
        // constructor tells the runtime whether the result already has the expected type
        MethodDef evaluate = evaluate(definition, compiler);
        if (definition.expectedType() == null) {
            ClassElement inferred = compiler.inferredEvaluationType();
            requireInferrable(inferred, definition.expression(), definition.node(), compiler, "expectedType");
            definition = definition.inferring(inferred);
        }
        boolean coerced = !compiler.evaluatesTo(definition.requireExpectedType());
        ClassDef.ClassDefBuilder builder = ClassDef.builder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .superclass(ClassTypeDef.of(CompiledValueExpression.class))
            // the source writer interprets a '$' in the javadoc
            .addJavadoc("The compiled form of the expression <code>" + definition.expression().replace("$", "$$") + "</code>.")
            .addMethod(constructor(definition, coerced))
            .addMethod(evaluate);

        ELNode node = unwrap(definition.node());
        if (isLValue(node)) {
            builder.addMethod(setValue(node, compiler));
            builder.addMethod(isReadOnly(node, compiler));
            builder.addMethod(getType(node, compiler));
            builder.addMethod(getValueReference(node, compiler));
        }
        return new Written(builder.build(), definition);
    }

    /**
     * An omitted type that inference resolved to {@link Object} because an identifier is not declared is an
     * error: the declaration is missing either the type of the identifier or the expected type.
     */
    static void requireInferrable(ClassElement inferred, String expression, ELNode node, ELCompiler compiler, String member) {
        if (!inferred.getName().equals(Object.class.getName())
            || compiler.unresolvedIdentifiers().isEmpty()
            || ELCompiler.hasAssignments(node)) {
            return;
        }
        String identifier = compiler.unresolvedIdentifiers().iterator().next();
        throw new ELCompilationException("Cannot infer the " + member + " of the expression '" + expression
            + "': the identifier '" + identifier + "' is not declared, so the expression has no static type."
            + " Declare the identifier with @ELVariable(name = \"" + identifier + "\", type = ...) in the"
            + " @ELEnvironment, or declare " + member + " (Object.class accepts any result).");
    }

    private static MethodDef constructor(ELExpressionDefinition definition, boolean coerced) {
        return MethodDef.constructor()
            .addModifiers(Modifier.PUBLIC)
            .build((aThis, parameters) -> aThis.superRef().invokeSuperConstructor(
                ExpressionDef.constant(definition.expression()),
                ExpressionDef.constant(ELNodes.canonical(definition.node())),
                ExpressionDef.constant(TypeDef.erasure(definition.requireExpectedType())),
                ExpressionDef.constant(coerced)
            ));
    }

    private static MethodDef evaluate(ELExpressionDefinition definition, ELCompiler compiler) {
        return MethodDef.builder("evaluate")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> compiler.compileEvaluation(definition.node(), parameters.get(0),
                context -> compiler.compileTyped(definition.node(), context)));
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
                // the constructor takes two Objects, and selecting it by the static types of the base and of
                // the property would emit a call to a constructor that does not exist
                return ClassTypeDef.of(ValueReference.class)
                    .instantiate(List.of(TypeDef.OBJECT, TypeDef.OBJECT), base, lValue.property())
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

    /**
     * A written expression class, with its definition: inference fills the expected type in.
     *
     * @param type       The generated class
     * @param definition The definition, its expected type resolved
     */
    public record Written(ClassDef type, ELExpressionDefinition definition) {
    }
}
