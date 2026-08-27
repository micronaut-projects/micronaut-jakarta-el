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

import io.micronaut.core.annotation.Experimental;
import org.jspecify.annotations.Nullable;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodNotFoundException;
import jakarta.el.MethodReference;
import jakarta.el.PropertyNotFoundException;

import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.Objects;

/**
 * The base class of the {@link MethodExpression} implementations generated at compilation time.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public abstract class CompiledMethodExpression extends MethodExpression implements CompiledExpression {

    private static final long serialVersionUID = 1L;
    private static final Class<?>[] NO_PARAM_TYPES = new Class<?>[0];

    private final String expressionString;
    private final String canonicalForm;
    private final @Nullable Class<?> expectedReturnType;
    private final Class<?> @Nullable [] expectedParamTypes;
    private final boolean parametersProvided;

    /**
     * @param expressionString    The original expression
     * @param expectedReturnType  The expected return type, or {@code null} to return the result without coercion
     * @param expectedParamTypes  The expected parameter types
     * @param parametersProvided  Whether the parameters are provided by the expression itself
     */
    protected CompiledMethodExpression(String expressionString,
                                       @Nullable Class<?> expectedReturnType,
                                       Class<?> @Nullable [] expectedParamTypes,
                                       boolean parametersProvided) {
        this(expressionString, expressionString, expectedReturnType, expectedParamTypes, parametersProvided);
    }

    /**
     * @param expressionString   The original expression
     * @param canonicalForm      The canonical form of the expression, which is what equality compares
     * @param expectedReturnType The expected return type, or {@code null} to return the result without coercion
     * @param expectedParamTypes The expected parameter types
     * @param parametersProvided Whether the parameters are provided by the expression itself
     */
    protected CompiledMethodExpression(String expressionString,
                                       String canonicalForm,
                                       @Nullable Class<?> expectedReturnType,
                                       Class<?> @Nullable [] expectedParamTypes,
                                       boolean parametersProvided) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.canonicalForm = Objects.requireNonNull(canonicalForm, "canonicalForm");
        this.expectedReturnType = expectedReturnType;
        this.expectedParamTypes = expectedParamTypes == null ? null : expectedParamTypes.clone();
        this.parametersProvided = parametersProvided;
    }

    /**
     * Evaluates the base object the method is invoked on, or the resolved method expression for an expression
     * consisting of a single identifier.
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
    protected abstract Object doInvoke(ELContext context, Object @Nullable [] arguments);

    /**
     * Evaluates the parameters an expression such as {@code ${bean.method(a, b)}} provides itself.
     *
     * @param context The context
     * @return The evaluated parameters, {@code null} for an expression that does not provide them
     */
    protected Object @Nullable [] evaluateArguments(ELContext context) {
        return null;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, Object @Nullable [] params) {
        context.notifyBeforeEvaluation(expressionString);
        Object result = doInvoke(context, params);
        Object coerced = expectedReturnType == null ? result
            : expectedReturnType == void.class ? null : ELSupport.coerceToType(context, result, expectedReturnType);
        context.notifyAfterEvaluation(expressionString);
        return coerced;
    }

    @Override
    public MethodInfo getMethodInfo(ELContext context) {
        Object base = evaluateBase(context);
        Object property = evaluateProperty(context);
        if (property == null && base instanceof MethodExpression expression) {
            return expression.getMethodInfo(context);
        }
        Method method = findMethod(base, property, evaluateArguments(context));
        return new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
    }

    @Override
    public MethodReference getMethodReference(ELContext context) {
        context.notifyBeforeEvaluation(expressionString);
        Object base = evaluateBase(context);
        Object property = evaluateProperty(context);
        if (property == null && base instanceof MethodExpression expression) {
            MethodReference reference = expression.getMethodReference(context);
            context.notifyAfterEvaluation(expressionString);
            return reference;
        }
        Object[] arguments = evaluateArguments(context);
        Method method = findMethod(base, property, arguments);
        MethodInfo methodInfo = new MethodInfo(method.getName(), method.getReturnType(), method.getParameterTypes());
        MethodReference reference = new MethodReference(base, methodInfo, method.getAnnotations(), arguments);
        context.notifyAfterEvaluation(expressionString);
        return reference;
    }

    @Override
    public boolean isParametersProvided() {
        return parametersProvided;
    }

    /**
     * Returns an equivalent expression that does not coerce its invocation result. Generated expression
     * registries use this when a caller supplies no expected return type.
     *
     * @return This expression when it is already uncoerced, or an uncoerced view
     * @since 1.0.1
     */
    public final MethodExpression withoutExpectedReturnType() {
        return expectedReturnType == null ? this : new UncoercedMethodExpression(this);
    }

    /**
     * @return The expected return type
     */
    protected Class<?> getExpectedReturnType() {
        return Objects.requireNonNull(expectedReturnType, "The expression has no expected return type");
    }

    /**
     * @return The expected parameter types
     */
    protected Class<?>[] getExpectedParamTypes() {
        return expectedParamTypes == null ? NO_PARAM_TYPES : expectedParamTypes.clone();
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
            && other.canonicalForm.equals(canonicalForm)
            && Objects.equals(other.expectedReturnType, expectedReturnType)
            && Arrays.equals(other.expectedParamTypes, expectedParamTypes);
    }

    @Override
    public int hashCode() {
        return Objects.hash(canonicalForm, expectedReturnType, Arrays.hashCode(expectedParamTypes));
    }

    @Override
    public String toString() {
        return "MethodExpression[" + expressionString + "]";
    }

    private Method findMethod(@Nullable Object base, @Nullable Object property, Object @Nullable [] arguments) {
        if (base == null) {
            throw new PropertyNotFoundException("Cannot resolve the base object of the expression '"
                + expressionString + "'");
        }
        if (property == null) {
            throw new MethodNotFoundException("Cannot resolve the method of the expression '" + expressionString + "'");
        }
        // the parameters provided by the expression select the method, the declared types otherwise
        Class<?>[] paramTypes = arguments == null ? getExpectedParamTypes() : null;
        String name = ELSupport.coerceToString(property);
        return base instanceof ELClass elClass
            ? ELMethods.findStaticMethod(elClass.getKlass(), name, paramTypes, arguments)
            : ELMethods.findMethod(base.getClass(), name, paramTypes, arguments);
    }

    private static final class UncoercedMethodExpression extends CompiledMethodExpression {

        private static final long serialVersionUID = 1L;

        private final CompiledMethodExpression delegate;

        private UncoercedMethodExpression(CompiledMethodExpression delegate) {
            super(delegate.expressionString, delegate.canonicalForm, null, delegate.expectedParamTypes,
                delegate.parametersProvided);
            this.delegate = delegate;
        }

        @Override
        @Nullable
        protected Object evaluateBase(ELContext context) {
            return delegate.evaluateBase(context);
        }

        @Override
        @Nullable
        protected Object evaluateProperty(ELContext context) {
            return delegate.evaluateProperty(context);
        }

        @Override
        @Nullable
        protected Object doInvoke(ELContext context, Object @Nullable [] arguments) {
            return delegate.doInvoke(context, arguments);
        }

        @Override
        protected Object @Nullable [] evaluateArguments(ELContext context) {
            return delegate.evaluateArguments(context);
        }
    }
}
