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
package io.micronaut.el.interpreter.reflection;

import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.FunctionMapper;
import jakarta.el.ImportHandler;
import jakarta.el.VariableMapper;
import org.jspecify.annotations.Nullable;

import java.util.Locale;

/**
 * The context one invocation of a lambda expression presented as a functional interface evaluates with.
 *
 * <p>An instance of the interface escapes the expression that coerced the lambda to it: a parallel stream, an
 * executor or the application can call it from several threads at once, while an {@link ELContext} is not safe
 * for concurrent use. Its lambda arguments are a stack and its resolution flag a field, so two threads entering
 * the lambda through the same context corrupt both. Each invocation therefore evaluates with a context of its
 * own, which delegates what is shared and read-only - the resolvers, the mappers, the import handler, the locale,
 * the context objects and the listeners - and owns what an evaluation mutates: the flag of a resolution and the
 * lambda arguments it enters. The arguments of the lambda expressions enclosing the coerced one are still read
 * from the context that coerced it.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
final class InvocationELContext extends ELContext {

    private final ELContext delegate;

    InvocationELContext(ELContext delegate) {
        this.delegate = delegate;
    }

    @Override
    public ELResolver getELResolver() {
        return delegate.getELResolver();
    }

    @Override
    @Nullable
    public FunctionMapper getFunctionMapper() {
        return delegate.getFunctionMapper();
    }

    @Override
    @Nullable
    public VariableMapper getVariableMapper() {
        return delegate.getVariableMapper();
    }

    @Override
    @Nullable
    public ImportHandler getImportHandler() {
        return delegate.getImportHandler();
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
    @Nullable
    public Object getContext(Class<?> key) {
        Object own = super.getContext(key);
        return own != null ? own : delegate.getContext(key);
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
        return super.isLambdaArgument(name) || delegate.isLambdaArgument(name);
    }

    @Override
    @Nullable
    public Object getLambdaArgument(String name) {
        return super.isLambdaArgument(name) ? super.getLambdaArgument(name) : delegate.getLambdaArgument(name);
    }
}
