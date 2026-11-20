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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.util.Optional;
import java.util.function.Function;

/**
 * The invocation of a method an {@link Optional} base object does not declare itself, as the
 * {@code jakarta.el.OptionalELResolver} of the specification performs it: such a method of an empty optional is
 * {@code null}, and of a present one the method of the value it holds. A method of {@code Optional} itself, such as
 * {@code map} or {@code orElse}, stays the method of the optional, as it is for a compiled expression, whose
 * resolver chain consults the reflective resolvers before the one of the optionals.
 *
 * <p>An expression compiled at compilation time reaches the resolver of the specification through the resolver
 * chain of its context. An expression parsed at runtime resolves its methods through the
 * {@link io.micronaut.el.ELMethodExecutor executors} instead, which is what this method is for: the method of the
 * value is resolved by the executors of the expression, so whatever sandbox they apply to the value applies to
 * it too.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELOptionalMethod implements ELMethod {

    private static final long serialVersionUID = 1L;

    private static final Argument<?>[] NO_ARGUMENTS = new Argument<?>[0];

    private final String name;
    @Nullable
    private final ELMethod method;

    private ELOptionalMethod(String name, @Nullable ELMethod method) {
        this.name = name;
        this.method = method;
    }

    /**
     * Resolves the method of an optional base object as the {@code OptionalELResolver} invokes it.
     *
     * @param optional The base object
     * @param name     The name of the method
     * @param resolver Resolves the method on the value the optional holds, when it holds one
     * @return The method, never {@code null}
     */
    public static ELMethod of(Optional<?> optional, Object name, Function<Object, ELMethod> resolver) {
        String methodName = String.valueOf(name);
        return optional.isEmpty()
            ? new ELOptionalMethod(methodName, null)
            : new ELOptionalMethod(methodName, resolver.apply(optional.get()));
    }

    @Override
    public String getName() {
        return method == null ? name : method.getName();
    }

    @Override
    public Argument<?> getReturnType() {
        return method == null ? Argument.OBJECT_ARGUMENT : method.getReturnType();
    }

    @Override
    public Argument<?>[] getArguments() {
        return method == null ? NO_ARGUMENTS : method.getArguments();
    }

    @Override
    public boolean isVarArgs() {
        return method != null && method.isVarArgs();
    }

    @Override
    public AnnotationMetadata getAnnotationMetadata() {
        return method == null ? AnnotationMetadata.EMPTY_METADATA : method.getAnnotationMetadata();
    }

    /**
     * Never reusable: whether the optional holds a value, and of what type, changes from one base object to the
     * next while the class of the base object stays {@link Optional}.
     *
     * @return {@code false}
     */
    @Override
    public boolean isReusable() {
        return false;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
        Object value = base instanceof Optional<?> optional ? optional.orElse(null) : base;
        if (value == null || method == null) {
            return null;
        }
        return method.invoke(context, value, arguments);
    }

    @Override
    public String identity() {
        return Optional.class.getName() + '#' + (method == null ? name : method.identity());
    }

    @Override
    public String toString() {
        return identity();
    }
}
