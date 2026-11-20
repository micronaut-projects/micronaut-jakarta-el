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
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.LambdaExpression;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * The invocation of a lambda expression presented as a Java functional interface, which may be called from any
 * thread, and from several at once: a parallel stream calls the function it was given from the threads of its
 * pool.
 *
 * <p>An {@link ELContext} is not safe to share between threads: the invocation of a lambda expression pushes its
 * arguments on the lambda scope of the context, and the resolution of every property flips its
 * {@code propertyResolved} flag. Each invocation through a functional interface is therefore given a context of its
 * own, which has its own lambda scope and flag and reads everything else - the resolvers, the variables, the
 * functions, the imports, the objects registered with {@link ELContext#putContext} and the arguments of the lambda
 * expressions enclosing the one invoked - from the context the expression is evaluated with. Nothing is written
 * to that context, so concurrent invocations only ever read it.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELLambdaInvocation {

    private ELLambdaInvocation() {
    }

    /**
     * Invokes a lambda expression from a functional interface, with a context of its own.
     *
     * @param context   The context the lambda expression was coerced with, or {@code null} to use the context
     *                  the lambda expression carries
     * @param lambda    The lambda expression
     * @param arguments The arguments
     * @return The result of the lambda expression
     */
    @Nullable
    public static Object invoke(@Nullable ELContext context, LambdaExpression lambda, Object... arguments) {
        ELContext parent = context;
        if (parent == null && lambda instanceof CompiledLambdaExpression compiled) {
            parent = compiled.context();
        }
        if (parent == null) {
            // the context the lambda expression carries is not readable from outside it
            return lambda.invoke(arguments);
        }
        return lambda.invoke(isolate(parent), arguments);
    }

    /**
     * A context reading everything from the given one but the lambda scope and the {@code propertyResolved} flag,
     * which are its own.
     *
     * @param context The context the expression is evaluated with
     * @return The context of one invocation
     */
    public static ELContext isolate(ELContext context) {
        return new InvocationContext(context);
    }

    /**
     * A context reading everything from the context a lambda expression is invoked with, except the variables,
     * which the variable mapper of the context the lambda expression was created in resolves first: a lambda
     * expression that escapes the expression it was created in keeps the bindings it closed over.
     *
     * <p>The variable mapper of the creation context is consulted when a variable is read, since a mapper cannot
     * list what it holds; a binding that mapper replaces later is the one read.</p>
     *
     * @param invoking The context the lambda expression is invoked with
     * @param creation The context the lambda expression was created in
     * @return The context to evaluate the body with
     */
    public static ELContext capturing(ELContext invoking, ELContext creation) {
        VariableMapper captured = creation.getVariableMapper();
        return captured == null ? invoking : new InvocationContext(invoking, captured);
    }

    /**
     * The context of one invocation of a lambda expression.
     */
    private static final class InvocationContext extends ELContext {

        private final ELContext parent;
        @Nullable
        private final VariableMapper captured;
        @Nullable
        private List<Map<String, Object>> scopes;

        private InvocationContext(ELContext parent) {
            this(parent, null);
        }

        private InvocationContext(ELContext parent, @Nullable VariableMapper captured) {
            this.parent = Objects.requireNonNull(parent, "context");
            this.captured = captured;
        }

        @Override
        public ELResolver getELResolver() {
            return parent.getELResolver();
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return parent.getFunctionMapper();
        }

        @Override
        public VariableMapper getVariableMapper() {
            VariableMapper own = parent.getVariableMapper();
            VariableMapper closed = captured;
            if (closed == null || closed == own) {
                return own;
            }
            return new VariableMapper() {
                @Override
                @Nullable
                public ValueExpression resolveVariable(String variable) {
                    ValueExpression expression = closed.resolveVariable(variable);
                    return expression != null || own == null ? expression : own.resolveVariable(variable);
                }

                @Override
                @Nullable
                public ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
                    return own == null ? null : own.setVariable(variable, expression);
                }
            };
        }

        @Override
        public ImportHandler getImportHandler() {
            return parent.getImportHandler();
        }

        @Override
        @Nullable
        public Object getContext(Class<?> key) {
            Object own = super.getContext(key);
            return own != null ? own : parent.getContext(key);
        }

        @Override
        public Locale getLocale() {
            Locale own = super.getLocale();
            return own != null ? own : parent.getLocale();
        }

        @Override
        public boolean isLambdaArgument(String arg) {
            List<Map<String, Object>> own = scopes;
            if (own != null) {
                for (int i = own.size() - 1; i >= 0; i--) {
                    if (own.get(i).containsKey(arg)) {
                        return true;
                    }
                }
            }
            return parent.isLambdaArgument(arg);
        }

        @Override
        @Nullable
        public Object getLambdaArgument(String arg) {
            List<Map<String, Object>> own = scopes;
            if (own != null) {
                for (int i = own.size() - 1; i >= 0; i--) {
                    Map<String, Object> scope = own.get(i);
                    if (scope.containsKey(arg)) {
                        return scope.get(arg);
                    }
                }
            }
            return parent.getLambdaArgument(arg);
        }

        @Override
        public void enterLambdaScope(Map<String, Object> arguments) {
            List<Map<String, Object>> own = scopes;
            if (own == null) {
                own = new ArrayList<>(2);
                scopes = own;
            }
            own.add(arguments);
        }

        @Override
        public void exitLambdaScope() {
            List<Map<String, Object>> own = scopes;
            if (own != null && !own.isEmpty()) {
                own.remove(own.size() - 1);
            }
        }
    }
}
