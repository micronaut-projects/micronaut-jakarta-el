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
package io.micronaut.el.resolver;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELArguments;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * Invokes the methods the {@link io.micronaut.el.ELMethodContributor} services registered from the resolver chain,
 * which is how an expression compiled at compilation time reaches a contributed method on a receiver whose type it
 * does not know: it dispatches through {@code ELContext.getELResolver().invoke}, and the resolver of the beans at
 * the end of the chain fails a method the class does not declare rather than declining it.
 *
 * <p>It sits where the contributions sit for the interpreter, before the resolvers of the specification, and
 * declines a call no contributed overload accepts, so a method the type declares itself is still found.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
final class ContributedELResolver extends ELResolver {

    private final ELMethodExecutor executor;

    /**
     * @param executor The executor of the contributed methods
     */
    ContributedELResolver(ELMethodExecutor executor) {
        this.executor = Objects.requireNonNull(executor, "executor");
    }

    @Override
    @Nullable
    public Object invoke(ELContext context,
                         @Nullable Object base,
                         @Nullable Object method,
                         Class<?> @Nullable [] paramTypes,
                         Object @Nullable [] params) {
        if (base == null || method == null) {
            return null;
        }
        ELMethod resolved = executor.resolve(context, base, method, ELArguments.of(paramTypes), params);
        if (resolved == null) {
            return null;
        }
        context.setPropertyResolved(base, method);
        return resolved.invoke(context, base, params);
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, @Nullable Object base, @Nullable Object property, @Nullable Object value) {
        // not a property resolver
    }

    @Override
    public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        return false;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return null;
    }
}
