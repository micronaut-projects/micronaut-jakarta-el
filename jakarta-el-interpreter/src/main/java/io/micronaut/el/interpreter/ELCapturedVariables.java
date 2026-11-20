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
package io.micronaut.el.interpreter;

import io.micronaut.core.annotation.Internal;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.EvaluationListener;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;

import java.util.Locale;
import java.util.Map;
import java.util.function.Function;

/**
 * Keeps the variables a lambda expression saw where it was created, for the invocations made with another
 * context.
 *
 * <p>The section 1.19 resolves the variables of an expression when the expression is created, and both reference
 * implementations replace them in the tree at that point: a lambda expression that escapes the expression creating
 * it, and is invoked with a context whose variable mapper does not know them, still sees them. The interpreter
 * resolves an identifier against the context it evaluates with, so the body of such a lambda evaluates against a
 * context whose variable mapper answers the free identifiers of the lambda with what the creating context
 * resolved them to.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
final class ELCapturedVariables {

    private ELCapturedVariables() {
    }

    /**
     * Wraps the body of a lambda expression being created so that it keeps the variables of the creating context.
     *
     * @param context   The context creating the lambda expression
     * @param freeNames The free identifiers of the lambda expression
     * @param body      The body
     * @return The body, wrapped when the creating context resolves one of the identifiers to a variable
     */
    static Function<ELContext, @Nullable Object> capture(ELContext context,
                                                         String[] freeNames,
                                                         Function<ELContext, @Nullable Object> body) {
        if (freeNames.length == 0) {
            return body;
        }
        VariableMapper mapper = context.getVariableMapper();
        if (mapper == null) {
            return body;
        }
        @Nullable ValueExpression[] captured = null;
        for (int i = 0; i < freeNames.length; i++) {
            ValueExpression variable = mapper.resolveVariable(freeNames[i]);
            if (variable != null) {
                if (captured == null) {
                    captured = new ValueExpression[freeNames.length];
                }
                captured[i] = variable;
            }
        }
        if (captured == null) {
            return body;
        }
        @Nullable ValueExpression[] variables = captured;
        return invocation -> invocation.getVariableMapper() == mapper
            ? body.apply(invocation)
            : body.apply(new CapturingContext(invocation, freeNames, variables));
    }

    /**
     * Delegates all of the state of the invoking context, the lambda arguments included, and only answers the
     * captured variables first.
     */
    private static final class CapturingContext extends ELContext {

        private final ELContext delegate;
        private final VariableMapper variableMapper;

        private CapturingContext(ELContext delegate, String[] names, @Nullable ValueExpression[] variables) {
            this.delegate = delegate;
            this.variableMapper = new CapturingVariableMapper(delegate.getVariableMapper(), names, variables);
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

    private static final class CapturingVariableMapper extends VariableMapper {

        private final @Nullable VariableMapper delegate;
        private final String[] names;
        private final @Nullable ValueExpression[] variables;

        private CapturingVariableMapper(@Nullable VariableMapper delegate,
                                        String[] names,
                                        @Nullable ValueExpression[] variables) {
            this.delegate = delegate;
            this.names = names;
            this.variables = variables;
        }

        @Override
        @Nullable
        public ValueExpression resolveVariable(String variable) {
            for (int i = 0; i < names.length; i++) {
                if (variables[i] != null && names[i].equals(variable)) {
                    return variables[i];
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
