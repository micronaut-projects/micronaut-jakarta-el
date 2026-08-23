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
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.ValueExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * A lambda expression whose body is compiled.
 *
 * <p>The invocation hands the arguments to the body directly: the body reads its parameters as Java locals, so
 * the lambda scope of the context, a map of the arguments by name pushed for every invocation by
 * {@link LambdaExpression#invoke(ELContext, Object...)}, is not needed. The nested lambda expressions capture the
 * parameters of the enclosing ones the way Java lambdas do, which is what the scope otherwise provides.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class CompiledLambdaExpression extends LambdaExpression {

    private final String[] parameters;
    private final ELLambdaBody body;
    @Nullable
    private ELContext context;

    /**
     * @param context    The context the lambda expression is created in
     * @param parameters The formal parameters
     * @param body       The compiled body
     */
    public CompiledLambdaExpression(ELContext context, String[] parameters, ELLambdaBody body) {
        super(List.of(parameters), new BodyValueExpression(parameters, body));
        this.parameters = parameters;
        this.body = body;
        this.context = context;
        super.setELContext(context);
    }

    @Override
    public void setELContext(ELContext context) {
        this.context = context;
        super.setELContext(context);
    }

    @Override
    @Nullable
    public Object invoke(ELContext elContext, Object... args) {
        if (args.length < parameters.length) {
            throw new ELException("Expected Argument " + parameters[args.length] + " missing in Lambda Expression");
        }
        return body.evaluate(elContext, args);
    }

    @Override
    @Nullable
    public Object invoke(Object... args) {
        ELContext elContext = context;
        if (elContext == null) {
            throw new ELException("The lambda expression has no context");
        }
        return invoke(elContext, args);
    }

    /**
     * The value expression of the body, which evaluates it with the arguments of the lambda scope. This is the
     * form {@link LambdaExpression#invoke} would use, kept so that the expression is complete for the code that
     * reaches it through the API.
     */
    private static final class BodyValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        private final String[] parameters;
        private final transient ELLambdaBody body;

        private BodyValueExpression(String[] parameters, ELLambdaBody body) {
            this.parameters = parameters;
            this.body = body;
        }

        @Override
        @SuppressWarnings("unchecked")
        @Nullable
        public <T> T getValue(ELContext context) {
            Object[] arguments = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                arguments[i] = context.getLambdaArgument(parameters[i]);
            }
            return (T) body.evaluate(context, arguments);
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
