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
import io.micronaut.el.processor.compiler.ELMethodExpressionDefinition;
import io.micronaut.el.parser.ELNodes;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.el.runtime.CompiledMethodExpression;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.el.ELContext;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.List;

/**
 * The writer of the {@code jakarta.el.MethodExpression} implementations.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class MethodExpressionWriter {

    private static final ClassTypeDef EL_CONTEXT = ClassTypeDef.of(ELContext.class);
    private static final ClassTypeDef EL_RESOLUTION = ClassTypeDef.of(ELResolution.class);
    private static final TypeDef.Array CLASS_ARRAY = TypeDef.array(
        TypeDef.parameterized(ClassTypeDef.of(Class.class), TypeDef.wildcard()));
    private static final String CONTEXT = "context";
    private static final String ARGUMENTS = "arguments";

    private MethodExpressionWriter() {
    }

    /**
     * Writes the implementation of a compiled method expression.
     *
     * @param className  The name of the generated class
     * @param definition The declared expression
     * @param compiler   The compiler
     * @return The definition of the generated class
     */
    public static Written write(String className,
                                ELMethodExpressionDefinition definition,
                                ELCompiler compiler) {
        ELNode node = unwrap(definition.node());
        if (definition.returnType() == null) {
            // an omitted return type is inferred from the static type of the invocation
            ClassElement inferred = compiler.inferredType(node);
            ValueExpressionWriter.requireInferrable(inferred, definition.expression(), node, compiler, "expectedReturnType");
            definition = definition.inferring(inferred);
        }
        ClassDef.ClassDefBuilder builder = ClassDef.builder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .superclass(ClassTypeDef.of(CompiledMethodExpression.class))
            // the source writer interprets a '$' in the javadoc
            .addJavadoc("The compiled form of the method expression <code>" + definition.expression().replace("$", "$$") + "</code>.")
            .addMethod(constructor(definition, node))
            .addMethod(evaluateBase(node, compiler))
            .addMethod(evaluateProperty(node, compiler))
            .addMethod(doInvoke(node, definition, compiler));
        if (node instanceof ELNode.Method method) {
            builder.addMethod(evaluateArguments(method, compiler));
        }
        return new Written(builder.build(), definition);
    }

    private static MethodDef constructor(ELMethodExpressionDefinition definition, ELNode node) {
        List<ExpressionDef> parameterTypes = definition.parameterTypes().stream()
            .map(type -> (ExpressionDef) ExpressionDef.constant(TypeDef.erasure(type)))
            .toList();
        return MethodDef.constructor()
            .addModifiers(Modifier.PUBLIC)
            .build((aThis, parameters) -> aThis.superRef().invokeSuperConstructor(
                ExpressionDef.constant(definition.expression()),
                ExpressionDef.constant(ELNodes.canonical(definition.node())),
                ExpressionDef.constant(TypeDef.erasure(definition.requireReturnType())),
                CLASS_ARRAY.instantiate(parameterTypes),
                ExpressionDef.constant(node instanceof ELNode.Method)
            ));
    }

    private static MethodDef evaluateBase(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("evaluateBase")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> base(node, compiler, parameters.get(0)).returning());
    }

    private static MethodDef evaluateProperty(ELNode node, ELCompiler compiler) {
        return MethodDef.builder("evaluateProperty")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> property(node, compiler, parameters.get(0)).returning());
    }

    private static MethodDef doInvoke(ELNode node,
                                      ELMethodExpressionDefinition definition,
                                      ELCompiler compiler) {
        return MethodDef.builder("doInvoke")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .addParameter(ARGUMENTS, TypeDef.OBJECT.array())
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ExpressionDef arguments = parameters.get(1);
                return switch (node) {
                    case ELNode.Method method -> compiler.compileEvaluation(method, context,
                        ctx -> new ELCompiler.Typed(compiler.invokeRuntime(EL_RESOLUTION, "invoke", TypeDef.OBJECT,
                            invocation(compiler, ctx, method).toArray(ExpressionDef[]::new)), null));
                    case ELNode.Property property -> compiler.invokeRuntime(EL_RESOLUTION, "invokeWithParamTypes", TypeDef.OBJECT,
                        context,
                        compiler.compile(property.base(), context),
                        compiler.compile(property.property(), context),
                        CLASS_ARRAY.instantiate(definition.parameterTypes().stream()
                            .map(type -> (ExpressionDef) ExpressionDef.constant(TypeDef.erasure(type)))
                            .toList()),
                        arguments).returning();
                    default -> compiler.invokeRuntime(EL_RESOLUTION, "invokeMethodExpression", TypeDef.OBJECT,
                        context, compiler.compile(node, context), arguments).returning();
                };
            });
    }

    private static List<ExpressionDef> invocation(ELCompiler compiler, ExpressionDef context, ELNode.Method method) {
        List<ExpressionDef> values = new ArrayList<>();
        values.add(context);
        values.add(compiler.compile(method.base(), context));
        values.add(compiler.compile(method.property(), context));
        method.arguments().forEach(argument -> values.add(compiler.compile(argument, context)));
        return values;
    }

    private static MethodDef evaluateArguments(ELNode.Method method, ELCompiler compiler) {
        return MethodDef.builder("evaluateArguments")
            .addModifiers(Modifier.PROTECTED)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .returns(TypeDef.OBJECT.array())
            .build((aThis, parameters) -> TypeDef.OBJECT.array()
                .instantiate(method.arguments().stream()
                    .map(argument -> compiler.compile(argument, parameters.get(0)))
                    .toList())
                .returning());
    }

    private static ExpressionDef base(ELNode node, ELCompiler compiler, ExpressionDef context) {
        return switch (node) {
            case ELNode.Method method -> compiler.compile(method.base(), context);
            case ELNode.Property property -> compiler.compile(property.base(), context);
            default -> compiler.compile(node, context);
        };
    }

    private static ExpressionDef property(ELNode node, ELCompiler compiler, ExpressionDef context) {
        return switch (node) {
            case ELNode.Method method -> compiler.compile(method.property(), context);
            case ELNode.Property property -> compiler.compile(property.property(), context);
            default -> ExpressionDef.nullValue();
        };
    }

    private static ELNode unwrap(ELNode node) {
        return node instanceof ELNode.Eval eval ? eval.expression() : node;
    }

    /**
     * A written method expression class, with its definition: inference fills the return type in.
     *
     * @param type       The generated class
     * @param definition The definition, its return type resolved
     */
    public record Written(ClassDef type, ELMethodExpressionDefinition definition) {
    }
}
