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
package io.micronaut.el.runtime;

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.MethodReference;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * The base class of the {@link MethodExpression} implementations generated at compilation time.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public abstract class CompiledMethodExpression extends MethodExpression implements CompiledExpression {

    private static final long serialVersionUID = 1L;
    private static final Class<?>[] NO_PARAM_TYPES = new Class<?>[0];

    private final String expressionString;
    private final Class<?> expectedReturnType;
    private final Class<?> @Nullable [] expectedParamTypes;
    private final boolean parametersProvided;

    /**
     * @param expressionString    The original expression
     * @param expectedReturnType  The expected return type
     * @param expectedParamTypes  The expected parameter types
     * @param parametersProvided  Whether the parameters are provided by the expression itself
     */
    protected CompiledMethodExpression(String expressionString,
                                       Class<?> expectedReturnType,
                                       @Nullable Class<?>[] expectedParamTypes,
                                       boolean parametersProvided) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.expectedReturnType = Objects.requireNonNull(expectedReturnType, "expectedReturnType");
        this.expectedParamTypes = expectedParamTypes;
        this.parametersProvided = parametersProvided;
    }

    /**
     * Evaluates the base object the method is invoked on, {@code null} for a method expression consisting
     * of a single identifier.
     *
     * @param context The context
     * @return The base object
     */
    @Nullable
    protected abstract Object evaluateBase(ELContext context);

    /**
     * @param context The context
     * @return The name of the invoked method
     */
    @Nullable
    protected abstract Object evaluateProperty(ELContext context);

    /**
     * Performs the compiled invocation.
     *
     * @param context   The context
     * @param arguments The arguments passed to {@link #invoke(ELContext, Object[])}
     * @return The result of the invocation, before the coercion to the expected return type
     */
    @Nullable
    protected abstract Object doInvoke(ELContext context, @Nullable Object[] arguments);

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object[] params) {
        context.notifyBeforeEvaluation(expressionString);
        Object result = doInvoke(context, params);
        context.notifyAfterEvaluation(expressionString);
        return expectedReturnType == void.class ? null : ELSupport.coerceToType(context, result, expectedReturnType);
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        Object base = evaluateBase(context);
        Object property = evaluateProperty(context);
        Method method = findMethod(base, property);
        return new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    @Override
    public MethodReference getMethodReference(ELContext context) {
        Object base = evaluateBase(context);
        Object property = evaluateProperty(context);
        Method method = findMethod(base, property);
        MethodInfo methodInfo = new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
        return new MethodReference(base, methodInfo, method.getAnnotations(), null);
    }

    @Override
    public boolean isParametersProvided() {
        return parametersProvided;
    }

    /**
     * @return The expected return type
     */
    protected Class<?> getExpectedReturnType() {
        return expectedReturnType;
    }

    /**
     * @return The expected parameter types
     */
    protected Class<?>[] getExpectedParamTypes() {
        return expectedParamTypes == null ? NO_PARAM_TYPES : expectedParamTypes;
    }

    @Override
    public final String getExpressionString() {
        return expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    @Override
    public boolean equals(Object obj) {
        return obj instanceof CompiledMethodExpression other
            && other.getClass() == getClass()
            && other.expressionString.equals(expressionString)
            && other.expectedReturnType.equals(expectedReturnType)
            && Arrays.equals(other.expectedParamTypes, expectedParamTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(getClass(), expressionString, expectedReturnType, Arrays.hashCode(expectedParamTypes));
    }

    @Override
    public String toString() {
        return "MethodExpression[" + expressionString + "]";
    }

    private Method findMethod(@Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw new MethodNotFoundException("Cannot resolve the method '" + property + "' of the expression '"
                + expressionString + "'");
        }
        String name = ELSupport.coerceToString(property);
        for (Method method : base.getClass().getMethods()) {
            if (method.getName().equals(name) && method.getParameterCount() == getExpectedParamTypes().length) {
                return method;
            }
        }
        throw new MethodNotFoundException("Cannot find the method '" + name + "' of "
            + base.getClass().getName() + " accepting " + getExpectedParamTypes().length + " argument(s)");
    }
}
