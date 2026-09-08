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
package io.micronaut.el;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.core.order.OrderUtil;
import io.micronaut.core.type.Argument;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The {@link ELMethodExecutor} services of the classpath, which describe a method without reflecting.
 *
 * <p>The interpreter builds its own list, since a parser can be constructed with the executors it may use.
 * This is the list a compiled expression reaches, which has no parser: it is what lets
 * {@code jakarta.el.MethodExpression} report the metadata of a method it never invokes reflectively.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELExecutors {

    private ELExecutors() {
    }

    /**
     * Resolves a method through the executors of the classpath.
     *
     * @param context       The context
     * @param base          The base object, or an {@code ELClass}
     * @param method        The method name
     * @param argumentTypes The parameter types declared by the expression, can be {@code null}
     * @param arguments     The evaluated arguments, can be {@code null}
     * @return The method, or {@code null} when no executor describes it
     */
    @Nullable
    public static ELMethod resolve(ELContext context,
                                   @Nullable Object base,
                                   @Nullable Object method,
                                   Argument<?> @Nullable [] argumentTypes,
                                   Object @Nullable [] arguments) {
        for (ELMethodExecutor executor : Holder.EXECUTORS) {
            ELMethod resolved = executor.resolve(context, base, method, argumentTypes, arguments);
            if (resolved != null) {
                return resolved;
            }
        }
        return null;
    }

    /**
     * Loaded on first use, so that nothing is read from the classpath until an expression needs it.
     */
    private static final class Holder {

        private static final List<ELMethodExecutor> EXECUTORS = load();

        private static List<ELMethodExecutor> load() {
            ClassLoader classLoader = Thread.currentThread().getContextClassLoader();
            List<ELMethodExecutor> executors = new ArrayList<>(
                SoftServiceLoader.load(ELMethodExecutor.class,
                    classLoader == null ? ELExecutors.class.getClassLoader() : classLoader).collectAll());
            OrderUtil.sort(executors);
            return List.copyOf(executors);
        }
    }
}
