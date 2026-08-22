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
import io.micronaut.el.resolver.ELBeanResolver;
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import io.micronaut.inject.ast.PropertyElement;
import io.micronaut.sourcegen.model.ClassDef;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import io.micronaut.sourcegen.model.FieldDef;
import io.micronaut.sourcegen.model.MethodDef;
import io.micronaut.sourcegen.model.StatementDef;
import io.micronaut.sourcegen.model.TypeDef;
import jakarta.el.ELContext;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;

import javax.lang.model.element.Modifier;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The writer of the {@link ELBeanResolver} implementations, which resolve the properties and the methods
 * of a bean without using reflection.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class BeanResolverWriter {

    private static final ClassTypeDef EL_CONTEXT = ClassTypeDef.of(ELContext.class);
    private static final ClassTypeDef EL_SUPPORT = ClassTypeDef.of(ELSupport.class);
    private static final TypeDef CLASS_TYPE = TypeDef.parameterized(ClassTypeDef.of(Class.class), TypeDef.wildcard());
    private static final TypeDef STRING_SET = TypeDef.parameterized(ClassTypeDef.of(Set.class),
        TypeDef.of(String.class));
    private static final String PROPERTY_NAMES = "PROPERTY_NAMES";
    private static final String CONTEXT = "context";
    private static final String BEAN = "bean";
    private static final String NAME = "name";
    private static final String VALUE = "value";
    private static final String ARGUMENTS = "arguments";

    private BeanResolverWriter() {
    }

    /**
     * Writes the resolver of a bean.
     *
     * @param className  The name of the generated class
     * @param beanType   The bean type
     * @param properties The resolvable properties
     * @param methods    The resolvable methods, by name and by number of arguments
     * @return The definition of the generated class
     */
    public static ClassDef write(String className,
                                 ClassElement beanType,
                                 List<PropertyElement> properties,
                                 Map<String, Map<Integer, MethodElement>> methods) {
        TypeDef beanTypeDef = TypeDef.erasure(beanType);
        return ClassDef.builder(className)
            .addModifiers(Modifier.PUBLIC, Modifier.FINAL)
            .addAnnotation(Generated.class)
            .addSuperinterface(ClassTypeDef.of(ELBeanResolver.class))
            .addJavadoc("The Jakarta Expression Language resolver of " + beanType.getName() + ".")
            .addField(propertyNamesField(properties))
            .addMethod(getBeanType(beanTypeDef))
            .addMethod(getPropertyNames(className))
            .addMethod(getPropertyType(properties))
            .addMethod(isReadOnly(properties))
            .addMethod(getProperty(beanType, beanTypeDef, properties))
            .addMethod(setProperty(beanType, beanTypeDef, properties))
            .addMethod(hasMethod(methods))
            .addMethod(invokeMethod(beanType, beanTypeDef, methods))
            .build();
    }

    private static FieldDef propertyNamesField(List<PropertyElement> properties) {
        List<ExpressionDef> names = properties.stream()
            .map(property -> (ExpressionDef) ExpressionDef.constant(property.getName()))
            .toList();
        return FieldDef.builder(PROPERTY_NAMES, STRING_SET)
            .addModifiers(Modifier.PRIVATE, Modifier.STATIC, Modifier.FINAL)
            .initializer(ClassTypeDef.of(Set.class).invokeStatic("of", STRING_SET, names))
            .build();
    }

    private static MethodDef getBeanType(TypeDef beanTypeDef) {
        return MethodDef.builder("getBeanType")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .returns(CLASS_TYPE)
            .build((aThis, parameters) -> ExpressionDef.constant(beanTypeDef).returning());
    }

    private static MethodDef getPropertyNames(String className) {
        return MethodDef.builder("getPropertyNames")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .returns(STRING_SET)
            .build((aThis, parameters) ->
                ClassTypeDef.of(className).getStaticField(PROPERTY_NAMES, STRING_SET).returning());
    }

    private static MethodDef getPropertyType(List<PropertyElement> properties) {
        return MethodDef.builder("getPropertyType")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(NAME, TypeDef.of(String.class))
            .returns(CLASS_TYPE)
            .build((aThis, parameters) -> {
                Map<ExpressionDef.Constant, ExpressionDef> cases = new LinkedHashMap<>();
                for (PropertyElement property : properties) {
                    cases.put(ExpressionDef.constant(property.getName()),
                        ExpressionDef.constant(TypeDef.erasure(property.getType())));
                }
                return parameters.get(0)
                    .asExpressionSwitch(CLASS_TYPE, cases, ExpressionDef.nullValue())
                    .returning();
            });
    }

    private static MethodDef isReadOnly(List<PropertyElement> properties) {
        return MethodDef.builder("isReadOnly")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(NAME, TypeDef.of(String.class))
            .returns(TypeDef.Primitive.BOOLEAN)
            .build((aThis, parameters) -> {
                Map<ExpressionDef.Constant, ExpressionDef> cases = new LinkedHashMap<>();
                for (PropertyElement property : properties) {
                    cases.put(ExpressionDef.constant(property.getName()),
                        ExpressionDef.constant(property.getWriteMethod().isEmpty()));
                }
                return parameters.get(0)
                    .asExpressionSwitch(TypeDef.Primitive.BOOLEAN, cases, ExpressionDef.constant(true))
                    .returning();
            });
    }

    private static MethodDef getProperty(ClassElement beanType,
                                         TypeDef beanTypeDef,
                                         List<PropertyElement> properties) {
        return MethodDef.builder("getProperty")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .addParameter(BEAN, TypeDef.OBJECT)
            .addParameter(NAME, TypeDef.of(String.class))
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> {
                ExpressionDef bean = parameters.get(1).cast(beanTypeDef);
                Map<ExpressionDef.Constant, StatementDef> cases = new LinkedHashMap<>();
                for (PropertyElement property : properties) {
                    MethodElement reader = property.getReadMethod().orElseThrow();
                    cases.put(ExpressionDef.constant(property.getName()), bean.invoke(reader).returning());
                }
                cases.put(null, ClassTypeDef.of(PropertyNotFoundException.class)
                    .instantiate(propertyMessage(beanType, parameters.get(2), "resolvable"))
                    .doThrow());
                return parameters.get(2).asStatementSwitch(TypeDef.OBJECT, cases);
            });
    }

    private static MethodDef setProperty(ClassElement beanType,
                                         TypeDef beanTypeDef,
                                         List<PropertyElement> properties) {
        return MethodDef.builder("setProperty")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .addParameter(BEAN, TypeDef.OBJECT)
            .addParameter(NAME, TypeDef.of(String.class))
            .addParameter(VALUE, TypeDef.OBJECT)
            .returns(TypeDef.VOID)
            .build((aThis, parameters) -> {
                ExpressionDef bean = parameters.get(1).cast(beanTypeDef);
                Map<ExpressionDef.Constant, StatementDef> cases = new LinkedHashMap<>();
                for (PropertyElement property : properties) {
                    MethodElement writer = property.getWriteMethod().orElse(null);
                    if (writer == null) {
                        cases.put(ExpressionDef.constant(property.getName()),
                            ClassTypeDef.of(PropertyNotWritableException.class)
                                .instantiate(propertyMessage(beanType, parameters.get(2), "writable"))
                                .doThrow());
                        continue;
                    }
                    ExpressionDef coerced = coerce(parameters.get(0), parameters.get(3),
                        writer.getParameters()[0].getType());
                    cases.put(ExpressionDef.constant(property.getName()), bean.invoke(writer, coerced));
                }
                cases.put(null, ClassTypeDef.of(PropertyNotFoundException.class)
                    .instantiate(propertyMessage(beanType, parameters.get(2), "resolvable"))
                    .doThrow());
                return parameters.get(2).asStatementSwitch(TypeDef.VOID, cases);
            });
    }

    private static MethodDef hasMethod(Map<String, Map<Integer, MethodElement>> methods) {
        return MethodDef.builder("hasMethod")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(NAME, TypeDef.of(String.class))
            .addParameter(ARGUMENTS, TypeDef.Primitive.INT)
            .returns(TypeDef.Primitive.BOOLEAN)
            .build((aThis, parameters) -> {
                Map<ExpressionDef.Constant, ExpressionDef> cases = new LinkedHashMap<>();
                methods.forEach((name, byCount) -> {
                    ExpressionDef.ConditionExpressionDef condition = null;
                    for (Integer count : byCount.keySet()) {
                        ExpressionDef.ConditionExpressionDef matches = parameters.get(1).compare(
                            ExpressionDef.ComparisonOperation.OpType.EQUAL_TO, ExpressionDef.constant(count.intValue()));
                        condition = condition == null ? matches : new ExpressionDef.Or(condition, matches);
                    }
                    cases.put(ExpressionDef.constant(name), condition);
                });
                return parameters.get(0)
                    .asExpressionSwitch(TypeDef.Primitive.BOOLEAN, cases, ExpressionDef.constant(false))
                    .returning();
            });
    }

    private static MethodDef invokeMethod(ClassElement beanType,
                                          TypeDef beanTypeDef,
                                          Map<String, Map<Integer, MethodElement>> methods) {
        return MethodDef.builder("invokeMethod")
            .addModifiers(Modifier.PUBLIC)
            .overrides()
            .addParameter(CONTEXT, EL_CONTEXT)
            .addParameter(BEAN, TypeDef.OBJECT)
            .addParameter(NAME, TypeDef.of(String.class))
            .addParameter(ARGUMENTS, TypeDef.OBJECT.array())
            .returns(TypeDef.OBJECT)
            .build((aThis, parameters) -> {
                ExpressionDef context = parameters.get(0);
                ExpressionDef bean = parameters.get(1).cast(beanTypeDef);
                ExpressionDef arguments = parameters.get(3);
                ExpressionDef count = EL_SUPPORT.invokeStatic("argumentCount", TypeDef.Primitive.INT, arguments);
                Map<ExpressionDef.Constant, StatementDef> cases = new LinkedHashMap<>();
                methods.forEach((name, byCount) -> {
                    Map<ExpressionDef.Constant, StatementDef> byArguments = new LinkedHashMap<>();
                    byCount.forEach((argumentCount, method) ->
                        byArguments.put(ExpressionDef.constant(argumentCount.intValue()),
                            invocation(bean, method, context, arguments)));
                    byArguments.put(null, methodNotFound(beanType, parameters.get(2)));
                    cases.put(ExpressionDef.constant(name), count.asStatementSwitch(TypeDef.OBJECT, byArguments));
                });
                cases.put(null, methodNotFound(beanType, parameters.get(2)));
                return parameters.get(2).asStatementSwitch(TypeDef.OBJECT, cases);
            });
    }

    private static StatementDef invocation(ExpressionDef bean,
                                           MethodElement method,
                                           ExpressionDef context,
                                           ExpressionDef arguments) {
        ParameterElement[] methodParameters = method.getParameters();
        List<ExpressionDef> values = new ArrayList<>(methodParameters.length);
        for (int i = 0; i < methodParameters.length; i++) {
            values.add(coerce(context, EL_SUPPORT.invokeStatic("argument", TypeDef.OBJECT, arguments,
                ExpressionDef.constant(i)), methodParameters[i].getType()));
        }
        ExpressionDef invocation = bean.invoke(method, values);
        if (method.getReturnType().isVoid()) {
            return StatementDef.multi((StatementDef) invocation, ExpressionDef.nullValue().returning());
        }
        return invocation.returning();
    }

    private static StatementDef methodNotFound(ClassElement beanType, ExpressionDef name) {
        return ClassTypeDef.of(MethodNotFoundException.class)
            .instantiate(ExpressionDef.constant("Cannot find the method '")
                .invoke("concat", TypeDef.of(String.class), name)
                .invoke("concat", TypeDef.of(String.class),
                    ExpressionDef.constant("' of " + beanType.getName())))
            .doThrow();
    }

    private static ExpressionDef propertyMessage(ClassElement beanType, ExpressionDef name, String suffix) {
        return ExpressionDef.constant("The property '")
            .invoke("concat", TypeDef.of(String.class), name)
            .invoke("concat", TypeDef.of(String.class),
                ExpressionDef.constant("' of " + beanType.getName() + " is not " + suffix));
    }

    private static ExpressionDef coerce(ExpressionDef context, ExpressionDef value, ClassElement target) {
        TypeDef targetType = TypeDef.erasure(target);
        String method = target.isInterface() ? "coerceToFunctionalInterface" : "coerceToType";
        return EL_SUPPORT.invokeStatic(method, targetType, context, value, ExpressionDef.constant(targetType));
    }
}
