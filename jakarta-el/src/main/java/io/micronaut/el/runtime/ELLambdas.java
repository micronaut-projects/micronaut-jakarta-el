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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import jakarta.el.ValueExpression;

import java.util.List;
import java.util.function.Function;

/**
 * The factory of the lambda expressions created by the expressions generated at compilation time.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELLambdas {

    private ELLambdas() {
    }

    /**
     * Creates a lambda expression described in the section 1.20 of the specification, whose body is evaluated
     * with the arguments in the lambda scope of the context. This is the form of the interpreter.
     *
     * @param context          The context
     * @param formalParameters The formal parameters
     * @param body             The body, reading the arguments from the context
     * @return The lambda expression
     */
    public static LambdaExpression create(ELContext context,
                                          List<String> formalParameters,
                                          Function<ELContext, @Nullable Object> body) {
        LambdaExpression lambdaExpression = new LambdaExpression(formalParameters, new BodyValueExpression(body));
        lambdaExpression.setELContext(context);
        return lambdaExpression;
    }

    /**
     * Creates a lambda expression without parameters from its compiled body.
     *
     * @param context The context
     * @param body    The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda0(ELContext context, ELLambdaBody.Nullary body) {
        return new CompiledLambdaExpression(context, new String[0], body);
    }

    /**
     * Creates a lambda expression with one parameter from its compiled body.
     *
     * @param context The context
     * @param first   The name of the parameter
     * @param body    The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda1(ELContext context, String first, ELLambdaBody.Unary body) {
        return new CompiledLambdaExpression(context, new String[] {first}, body);
    }

    /**
     * Creates a lambda expression with two parameters from its compiled body.
     *
     * @param context The context
     * @param first   The name of the first parameter
     * @param second  The name of the second parameter
     * @param body    The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda2(ELContext context, String first, String second, ELLambdaBody.Binary body) {
        return new CompiledLambdaExpression(context, new String[] {first, second}, body);
    }

    /**
     * Creates a lambda expression with three parameters from its compiled body.
     *
     * @param context The context
     * @param first   The name of the first parameter
     * @param second  The name of the second parameter
     * @param third   The name of the third parameter
     * @param body    The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda3(ELContext context, String first, String second, String third, ELLambdaBody.Ternary body) {
        return new CompiledLambdaExpression(context, new String[] {first, second, third}, body);
    }

    /**
     * Creates a lambda expression with any number of parameters from its compiled body. The names are passed as
     * an array, a generic list has a descriptor the source and the bytecode writers do not infer alike.
     *
     * @param context          The context
     * @param formalParameters The formal parameters
     * @param body             The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda(ELContext context, String[] formalParameters, ELLambdaBody body) {
        return new CompiledLambdaExpression(context, formalParameters, body);
    }

    /**
     * Discards the result of the body of a lambda expression implementing a functional interface whose method
     * returns nothing.
     *
     * @param ignored The result
     */
    public static void discard(@Nullable Object ignored) {
        // the body is evaluated for its effects
    }

    /**
     * @param arguments The arguments of a lambda expression
     * @param index     The index of a parameter
     * @return The argument
     */
    @Nullable
    public static Object argument(@Nullable Object[] arguments, int index) {
        return arguments[index];
    }

    /**
     * The value expression holding the body of a lambda expression of the interpreter.
     */
    private static final class BodyValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        private final transient Function<ELContext, @Nullable Object> body;

        private BodyValueExpression(Function<ELContext, @Nullable Object> body) {
            this.body = body;
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getValue(ELContext context) {
            return (T) body.apply(context);
        }

        @Override
        public void setValue(ELContext context, @Nullable Object value) {
            throw new UnsupportedOperationException("The body of a lambda expression is not an lvalue");
        }

        @Override
        public boolean isReadOnly(ELContext context) {
            return true;
        }

        @Override
        @Nullable
        public Class<?> getType(ELContext context) {
            return null;
        }

        @Override
        public Class<?> getExpectedType() {
            return Object.class;
        }

        @Override
        @Nullable
        public String getExpressionString() {
            return null;
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof BodyValueExpression other && other.body.equals(body);
        }

        @Override
        public int hashCode() {
            return body.hashCode();
        }

        @Override
        public boolean isLiteralText() {
            return false;
        }
    }
}
