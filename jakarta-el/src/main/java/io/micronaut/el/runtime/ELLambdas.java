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
     * Creates a lambda expression described in the section 1.20 of the specification.
     *
     * @param context          The context
     * @param formalParameters The formal parameters
     * @param body             The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression create(ELContext context,
                                          List<String> formalParameters,
                                          ELLambdaBody body) {
        LambdaExpression lambdaExpression = new LambdaExpression(formalParameters, new BodyValueExpression(body));
        lambdaExpression.setELContext(context);
        return lambdaExpression;
    }

    /**
     * Creates a lambda expression described in the section 1.20 of the specification. This is the form the
     * generated code calls: the methods the generated code calls are never overloaded, and take arrays rather
     * than generic collections, so that their descriptors are the same in source and in bytecode.
     *
     * @param context          The context
     * @param formalParameters The formal parameters
     * @param body             The compiled body
     * @return The lambda expression
     */
    public static LambdaExpression lambda(ELContext context, String[] formalParameters, ELLambdaBody body) {
        return create(context, List.of(formalParameters), body);
    }

    /**
     * The value expression holding the compiled body of a lambda expression.
     */
    private static final class BodyValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        private final transient ELLambdaBody body;

        private BodyValueExpression(ELLambdaBody body) {
            this.body = body;
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getValue(ELContext context) {
            return (T) body.evaluate(context);
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
