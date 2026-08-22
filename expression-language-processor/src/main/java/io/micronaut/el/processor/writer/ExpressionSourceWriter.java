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
import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.processor.compiler.ELExpressionDefinition;
import io.micronaut.el.processor.compiler.ELMethodExpressionDefinition;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The writer of the {@link ELExpressionSource} implementations, which give access to the expressions
 * compiled for a type.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ExpressionSourceWriter {

    private static final ClassTypeDef VALUE_EXPRESSION = ClassTypeDef.of(ValueExpression.class);
    private static final ClassTypeDef METHOD_EXPRESSION = ClassTypeDef.of(MethodExpression.class);
    private static final TypeDef CLASS_TYPE = TypeDef.parameterized(ClassTypeDef.of(Class.class), TypeDef.wildcard());
    private static final TypeDef.Array CLASS_ARRAY = TypeDef.array(CLASS_TYPE);
    private static final TypeDef STRING = TypeDef.of(String.class);
    private static final String EXPRESSION = "expression";

    private ExpressionSourceWriter() {
    }

    /**
     * Writes the source of the expressions compiled for a type.
     *
     * @param className         The name of the generated class
     * @param valueExpressions  The compiled value expressions
     * @param methodExpressions The compiled method expressions
     * @return The definition of the generated class
     */
    public static ClassDef write(String className,
                                 List<CompiledValue> valueExpressions,
                                 List<CompiledMethod> methodExpressions) {
        ClassDef.ClassDefBuilder builder = ClassDef.builder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .addSuperinterface(ClassTypeDef.of(ELExpressionSource.class))
            .addJavadoc("The expressions compiled at compilation time.");

        for (CompiledValue value : valueExpressions) {
            builder.addField(constant(value.definition().constantName(), VALUE_EXPRESSION, value.className()));
        }
        for (CompiledMethod method : methodExpressions) {
            builder.addField(constant(method.definition().constantName(), METHOD_EXPRESSION, method.className()));
        }
        if (!valueExpressions.isEmpty()) {
            builder.addMethod(createValueExpression(className, valueExpressions));
        }
        if (!methodExpressions.isEmpty()) {
            builder.addMethod(createMethodExpression(className, methodExpressions));
        }
        return builder.build();
    }

    private static FieldDef constant(String name, ClassTypeDef type, String implementationClass) {
        return FieldDef.builder(name, type)
            .addModifiers(Modifier.PUBLIC, Modifier.STATIC, Modifier.FINAL)
            .initializer(ClassTypeDef.of(implementationClass).instantiate())
            .build();
    }

    private static MethodDef createValueExpression(String className, List<CompiledValue> expressions) {
        return MethodDef.builder("createValueExpression")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(EXPRESSION, STRING)
            .addParameter("expectedType", CLASS_TYPE)
            .returns(VALUE_EXPRESSION)
            .build((aThis, parameters) -> {
                Map<String, List<CompiledValue>> byExpression = new LinkedHashMap<>();
                for (CompiledValue value : expressions) {
                    byExpression.computeIfAbsent(value.definition().expression(), key -> new ArrayList<>()).add(value);
                }
                Map<ExpressionDef.Constant, ExpressionDef> cases = new LinkedHashMap<>();
                byExpression.forEach((expression, values) -> {
                    ExpressionDef result = ExpressionDef.nullValue();
                    for (int i = values.size() - 1; i >= 0; i--) {
                        CompiledValue value = values.get(i);
                        result = new ExpressionDef.IfElse(
                            ExpressionDef.constant(TypeDef.erasure(value.definition().expectedType()))
                                .equalsReferentially(parameters.get(1)),
                            ClassTypeDef.of(className)
                                .getStaticField(value.definition().constantName(), VALUE_EXPRESSION),
                            result
                        );
                    }
                    cases.put(ExpressionDef.constant(expression), result);
                });
                return parameters.get(0)
                    .asExpressionSwitch(VALUE_EXPRESSION, cases, ExpressionDef.nullValue())
                    .returning();
            });
    }

    private static MethodDef createMethodExpression(String className, List<CompiledMethod> expressions) {
        return MethodDef.builder("createMethodExpression")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(EXPRESSION, STRING)
            .addParameter("expectedReturnType", CLASS_TYPE)
            .addParameter("expectedParamTypes", CLASS_ARRAY)
            .returns(METHOD_EXPRESSION)
            .build((aThis, parameters) -> {
                Map<String, List<CompiledMethod>> byExpression = new LinkedHashMap<>();
                for (CompiledMethod method : expressions) {
                    byExpression.computeIfAbsent(method.definition().expression(), key -> new ArrayList<>())
                        .add(method);
                }
                Map<ExpressionDef.Constant, ExpressionDef> cases = new LinkedHashMap<>();
                byExpression.forEach((expression, methods) -> {
                    ExpressionDef result = ExpressionDef.nullValue();
                    for (int i = methods.size() - 1; i >= 0; i--) {
                        CompiledMethod method = methods.get(i);
                        List<ExpressionDef> parameterTypes = method.definition().parameterTypes().stream()
                            .map(type -> (ExpressionDef) ExpressionDef.constant(TypeDef.erasure(type)))
                            .toList();
                        ExpressionDef.ConditionExpressionDef matches = new ExpressionDef.And(
                            ExpressionDef.constant(TypeDef.erasure(method.definition().returnType()))
                                .equalsReferentially(parameters.get(1)),
                            ClassTypeDef.of(Arrays.class).invokeStatic("equals", TypeDef.Primitive.BOOLEAN,
                                CLASS_ARRAY.instantiate(parameterTypes), parameters.get(2)).isTrue()
                        );
                        result = new ExpressionDef.IfElse(
                            matches,
                            ClassTypeDef.of(className)
                                .getStaticField(method.definition().constantName(), METHOD_EXPRESSION),
                            result
                        );
                    }
                    cases.put(ExpressionDef.constant(expression), result);
                });
                return parameters.get(0)
                    .asExpressionSwitch(METHOD_EXPRESSION, cases, ExpressionDef.nullValue())
                    .returning();
            });
    }

    /**
     * A compiled value expression and the class generated for it.
     *
     * @param definition The declared expression
     * @param className  The name of the generated class
     */
    public record CompiledValue(ELExpressionDefinition definition, String className) {
    }

    /**
     * A compiled method expression and the class generated for it.
     *
     * @param definition The declared expression
     * @param className  The name of the generated class
     */
    public record CompiledMethod(ELMethodExpressionDefinition definition, String className) {
    }
}
