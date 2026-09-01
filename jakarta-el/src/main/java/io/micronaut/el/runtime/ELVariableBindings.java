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
import io.micronaut.core.annotation.UsedByGeneratedCode;
import jakarta.el.ELContext;
import jakarta.el.Expression;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.MethodExpression;
import jakarta.el.MethodInfo;
import jakarta.el.MethodReference;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Captures the variable expressions resolved while an expression is created, as required by section 1.19.
 */
@Internal
public final class ELVariableBindings {

    private ELVariableBindings() {
    }

    /**
     * Returns the parsed expression behind a creation-time variable-binding view. Equality is defined by the
     * parsed representation, and the bindings do not change it.
     *
     * @param expression The expression or binding view
     * @return The underlying expression
     */
    public static Expression unwrap(Expression expression) {
        if (expression instanceof BoundValueExpression bound) {
            return unwrap(bound.delegate);
        }
        if (expression instanceof BoundMethodExpression bound) {
            return unwrap(bound.delegate);
        }
        return expression;
    }

    /**
     * Binds the named variables of a value expression against its creation context.
     *
     * @param context    The creation context
     * @param expression The expression
     * @param names      The free identifiers of the expression
     * @return The bound expression
     */
    @UsedByGeneratedCode
    public static ValueExpression bind(@Nullable ELContext context,
                                       ValueExpression expression,
                                       String... names) {
        Bindings bindings = Bindings.capture(context, names);
        if (bindings == null) {
            return expression;
        }
        return expression instanceof CompiledExpression
            ? new BoundCompiledValueExpression(expression, bindings)
            : new BoundValueExpression(expression, bindings);
    }

    /**
     * Binds a value expression selected by a generated registry, preserving a missing selection.
     *
     * @param context    The creation context
     * @param expression The selected expression, possibly {@code null}
     * @param names      The free identifiers of the expression
     * @return The bound expression, or {@code null}
     */
    @UsedByGeneratedCode
    @Nullable
    public static ValueExpression bindNullable(@Nullable ELContext context,
                                               @Nullable ValueExpression expression,
                                               String... names) {
        return expression == null ? null : bind(context, expression, names);
    }

    /**
     * Binds the named variables of a method expression against its creation context.
     *
     * @param context    The creation context
     * @param expression The expression
     * @param names      The free identifiers of the expression
     * @return The bound expression
     */
    @UsedByGeneratedCode
    public static MethodExpression bind(@Nullable ELContext context,
                                        MethodExpression expression,
                                        String... names) {
        Bindings bindings = Bindings.capture(context, names);
        if (bindings == null) {
            return expression;
        }
        return expression instanceof CompiledExpression
            ? new BoundCompiledMethodExpression(expression, bindings)
            : new BoundMethodExpression(expression, bindings);
    }

    /**
     * Binds a method expression selected by a generated registry, preserving a missing selection.
     *
     * @param context    The creation context
     * @param expression The selected expression, possibly {@code null}
     * @param names      The free identifiers of the expression
     * @return The bound expression, or {@code null}
     */
    @UsedByGeneratedCode
    @Nullable
    public static MethodExpression bindNullable(@Nullable ELContext context,
                                                @Nullable MethodExpression expression,
                                                String... names) {
        return expression == null ? null : bind(context, expression, names);
    }

    private static final class Bindings implements java.io.Serializable {

        private static final long serialVersionUID = 1L;

        private final String[] names;
        private final @Nullable ValueExpression[] expressions;

        private Bindings(String[] names, @Nullable ValueExpression[] expressions) {
            this.names = names;
            this.expressions = expressions;
        }

        @Nullable
        private static Bindings capture(@Nullable ELContext context, String[] names) {
            if (context == null || names.length == 0) {
                return null;
            }
            String[] copiedNames = names.clone();
            @Nullable ValueExpression[] expressions = new ValueExpression[copiedNames.length];
            VariableMapper mapper = context.getVariableMapper();
            if (mapper != null) {
                for (int i = 0; i < copiedNames.length; i++) {
                    expressions[i] = mapper.resolveVariable(copiedNames[i]);
                }
            }
            return new Bindings(copiedNames, expressions);
        }

    }

    private static class BoundValueExpression extends ValueExpression {

        private static final long serialVersionUID = 1L;

        private final ValueExpression delegate;
        private final Bindings bindings;

        private BoundValueExpression(ValueExpression delegate, Bindings bindings) {
            this.delegate = delegate;
            this.bindings = bindings;
        }

        @Override
        @Nullable
        public <T> T getValue(ELContext context) {
            return delegate.getValue(new BindingContext(context, bindings));
        }

        @Override
        public void setValue(ELContext context, @Nullable Object value) {
            delegate.setValue(new BindingContext(context, bindings), value);
        }

        @Override
        public boolean isReadOnly(ELContext context) {
            return delegate.isReadOnly(new BindingContext(context, bindings));
        }

        @Override
        @Nullable
        public Class<?> getType(ELContext context) {
            return delegate.getType(new BindingContext(context, bindings));
        }

        @Override
        @Nullable
        public ValueReference getValueReference(ELContext context) {
            return delegate.getValueReference(new BindingContext(context, bindings));
        }

        @Override
        public Class<?> getExpectedType() {
            return delegate.getExpectedType();
        }

        @Override
        @Nullable
        public String getExpressionString() {
            return delegate.getExpressionString();
        }

        @Override
        public boolean isLiteralText() {
            return delegate.isLiteralText();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof ValueExpression expression
                && delegate.equals(ELVariableBindings.unwrap(expression));
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    private static final class BoundCompiledValueExpression extends BoundValueExpression implements CompiledExpression {

        private static final long serialVersionUID = 1L;

        private BoundCompiledValueExpression(ValueExpression delegate, Bindings bindings) {
            super(delegate, bindings);
        }
    }

    private static class BoundMethodExpression extends MethodExpression {

        private static final long serialVersionUID = 1L;

        private final MethodExpression delegate;
        private final Bindings bindings;

        private BoundMethodExpression(MethodExpression delegate, Bindings bindings) {
            this.delegate = delegate;
            this.bindings = bindings;
        }

        @Override
        public MethodInfo getMethodInfo(ELContext context) {
            return delegate.getMethodInfo(new BindingContext(context, bindings));
        }

        @Override
        @Nullable
        public MethodReference getMethodReference(ELContext context) {
            return delegate.getMethodReference(new BindingContext(context, bindings));
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, Object @Nullable [] params) {
            return delegate.invoke(new BindingContext(context, bindings), params);
        }

        @Override
        public boolean isParametersProvided() {
            return delegate.isParametersProvided();
        }

        @Override
        public String getExpressionString() {
            return delegate.getExpressionString();
        }

        @Override
        public boolean isLiteralText() {
            return delegate.isLiteralText();
        }

        @Override
        public boolean equals(@Nullable Object obj) {
            return obj instanceof MethodExpression expression
                && delegate.equals(ELVariableBindings.unwrap(expression));
        }

        @Override
        public int hashCode() {
            return delegate.hashCode();
        }
    }

    private static final class BoundCompiledMethodExpression extends BoundMethodExpression implements CompiledExpression {

        private static final long serialVersionUID = 1L;

        private BoundCompiledMethodExpression(MethodExpression delegate, Bindings bindings) {
            super(delegate, bindings);
        }
    }

    /**
     * Delegates all context state while replacing only the mapper view used by the bound expression.
     */
    private static final class BindingContext extends ELContext {

        private final ELContext delegate;
        private final VariableMapper variableMapper;

        private BindingContext(ELContext delegate, Bindings bindings) {
            this.delegate = Objects.requireNonNull(delegate, "delegate");
            this.variableMapper = new BindingVariableMapper(delegate.getVariableMapper(), bindings);
        }

        @Override
        public void setPropertyResolved(boolean resolved) {
            delegate.setPropertyResolved(resolved);
        }

        @Override
        public void setPropertyResolved(@Nullable Object base, @Nullable Object property) {
            delegate.setPropertyResolved(base, property);
        }

        @Override
        public boolean isPropertyResolved() {
            return delegate.isPropertyResolved();
        }

        @Override
        public void putContext(Class<?> key, Object contextObject) {
            delegate.putContext(key, contextObject);
        }

        @Override
        @Nullable
        public Object getContext(Class<?> key) {
            return delegate.getContext(key);
        }

        @Override
        public ELResolver getELResolver() {
            return delegate.getELResolver();
        }

        @Override
        @Nullable
        public ImportHandler getImportHandler() {
            return delegate.getImportHandler();
        }

        @Override
        @Nullable
        public FunctionMapper getFunctionMapper() {
            return delegate.getFunctionMapper();
        }

        @Override
        @Nullable
        public Locale getLocale() {
            return delegate.getLocale();
        }

        @Override
        public void setLocale(@Nullable Locale locale) {
            delegate.setLocale(locale);
        }

        @Override
        public VariableMapper getVariableMapper() {
            return variableMapper;
        }

        @Override
        public void addEvaluationListener(EvaluationListener listener) {
            delegate.addEvaluationListener(listener);
        }

        @Override
        public void notifyBeforeEvaluation(String expression) {
            delegate.notifyBeforeEvaluation(expression);
        }

        @Override
        public void notifyAfterEvaluation(String expression) {
            delegate.notifyAfterEvaluation(expression);
        }

        @Override
        public void notifyPropertyResolved(@Nullable Object base, @Nullable Object property) {
            delegate.notifyPropertyResolved(base, property);
        }

        @Override
        public boolean isLambdaArgument(String name) {
            return delegate.isLambdaArgument(name);
        }

        @Override
        @Nullable
        public Object getLambdaArgument(String name) {
            return delegate.getLambdaArgument(name);
        }

        @Override
        public void enterLambdaScope(Map<String, Object> arguments) {
            delegate.enterLambdaScope(arguments);
        }

        @Override
        public void exitLambdaScope() {
            delegate.exitLambdaScope();
        }

        @Override
        @Nullable
        public <T> T convertToType(@Nullable Object value, Class<T> type) {
            return delegate.convertToType(value, type);
        }
    }

    private static final class BindingVariableMapper extends VariableMapper {

        private final @Nullable VariableMapper delegate;
        private final String[] names;
        private final @Nullable ValueExpression[] expressions;

        private BindingVariableMapper(@Nullable VariableMapper delegate, Bindings bindings) {
            this.delegate = delegate;
            this.names = bindings.names;
            this.expressions = bindings.expressions;
        }

        @Override
        @Nullable
        public ValueExpression resolveVariable(String variable) {
            for (int i = 0; i < names.length; i++) {
                if (names[i].equals(variable)) {
                    return expressions[i];
                }
            }
            return delegate == null ? null : delegate.resolveVariable(variable);
        }

        @Override
        @Nullable
        public ValueExpression setVariable(String variable, @Nullable ValueExpression expression) {
            return delegate == null ? null : delegate.setVariable(variable, expression);
        }
    }
}
