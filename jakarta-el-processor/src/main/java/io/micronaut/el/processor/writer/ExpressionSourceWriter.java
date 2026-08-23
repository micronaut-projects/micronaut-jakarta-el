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
import io.micronaut.core.reflect.ClassUtils;
import io.micronaut.core.reflect.ReflectionUtils;
import io.micronaut.inject.ast.ClassElement;
import org.jspecify.annotations.Nullable;
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
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
    private static final TypeDef.Array STRING_ARRAY = TypeDef.array(STRING);
    private static final TypeDef STRING_LIST = TypeDef.parameterized(ClassTypeDef.of(List.class), STRING);
    private static final TypeDef.Array OBJECT_ARRAY = TypeDef.array(TypeDef.OBJECT);
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
        builder.addMethod(expressions(valueExpressions, methodExpressions));
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

    /**
     * The expression strings of the source, without duplicates: an expression declared twice with different
     * expected types is one string, and a string declared both as a value and as a method expression is one
     * string too.
     */
    private static MethodDef expressions(List<CompiledValue> valueExpressions, List<CompiledMethod> methodExpressions) {
        Set<String> strings = new LinkedHashSet<>();
        for (CompiledValue value : valueExpressions) {
            strings.add(value.definition().expression());
        }
        for (CompiledMethod method : methodExpressions) {
            strings.add(method.definition().expression());
        }
        List<ExpressionDef> constants = strings.stream().map(s -> (ExpressionDef) ExpressionDef.constant(s)).toList();
        // List.of(E...) erases to List.of(Object[]): the parameter type is given explicitly so that the bytecode
        // generator emits that descriptor rather than List.of(String[]), which does not exist.
        return MethodDef.builder("expressions")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .returns(STRING_LIST)
            .build((aThis, parameters) -> ClassTypeDef.of(List.class)
                .invokeStatic("of", List.of(OBJECT_ARRAY), STRING_LIST, List.of(STRING_ARRAY.instantiate(constants)))
                .returning());
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
                            requestedType(value.definition().requireExpectedType(), value.definition().inferred(), parameters.get(1)),
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
                            requestedType(method.definition().requireReturnType(), method.definition().inferred(), parameters.get(1)),
                            // the declared parameters are Object[]: the descriptor must not be inferred from
                            // the arguments, which the bytecode writer would emit as it is
                            ClassTypeDef.of(Arrays.class).invokeStatic("equals", List.of(OBJECT_ARRAY, OBJECT_ARRAY),
                                TypeDef.Primitive.BOOLEAN,
                                List.of(CLASS_ARRAY.instantiate(parameterTypes), parameters.get(2))).isTrue()
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
    /**
     * Whether the requested type is the declared one, a primitive and its wrapper being the same expectation:
     * the coercion rules treat them alike, and the languages do not agree on which one an annotation names,
     * KSP reads a Kotlin {@code Double::class} as the wrapper while {@code Double::class.java} is the primitive.
     */
    /**
     * Whether the requested type selects the expression: its declared type, or, for a declaration whose type
     * was inferred rather than written, also {@link Object}, the type a caller passes when it does not care.
     */
    private static ExpressionDef.ConditionExpressionDef requestedType(ClassElement declared, boolean inferred, ExpressionDef requested) {
        ExpressionDef.ConditionExpressionDef matches = sameType(declared, requested);
        if (inferred && !declared.getName().equals(Object.class.getName())) {
            matches = new ExpressionDef.Or(matches, ExpressionDef.constant(TypeDef.OBJECT).equalsReferentially(requested));
        }
        return matches;
    }

    private static ExpressionDef.ConditionExpressionDef sameType(ClassElement declared, ExpressionDef requested) {
        TypeDef type = TypeDef.erasure(declared);
        ExpressionDef.ConditionExpressionDef same = ExpressionDef.constant(type).equalsReferentially(requested);
        TypeDef counterpart = counterpartOf(declared);
        if (counterpart == null) {
            return same;
        }
        return new ExpressionDef.Or(same, ExpressionDef.constant(counterpart).equalsReferentially(requested));
    }

    @Nullable
    private static TypeDef counterpartOf(ClassElement declared) {
        String name = declared.getName();
        if (declared.isArray() || name.equals("void")) {
            return null;
        }
        if (declared.isPrimitive()) {
            return ClassUtils.getPrimitiveType(name)
                .<TypeDef>map(primitive -> ClassTypeDef.of(ReflectionUtils.getWrapperType(primitive)))
                .orElse(null);
        }
        return ClassUtils.forName(name, null)
            .map(ReflectionUtils::getPrimitiveType)
            .filter(Class::isPrimitive)
            .<TypeDef>map(TypeDef::primitive)
            .orElse(null);
    }

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
