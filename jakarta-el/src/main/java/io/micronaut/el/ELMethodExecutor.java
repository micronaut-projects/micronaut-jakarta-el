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

import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.type.Argument;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

/**
 * Resolves and executes methods for expressions parsed at runtime.
 *
 * <p>Implementations are discovered as services by the interpreter. A provider can use generated code, a
 * registry, or another direct dispatch mechanism. Reflection is intentionally not part of this contract; it is
 * supplied separately by the optional interpreter-reflection module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Experimental
public interface ELMethodExecutor extends Ordered {

    /**
     * Returns the executor order. Lower values run first, following the Micronaut {@link Ordered} contract, so
     * direct/generated implementations can take precedence over a general fallback such as reflection.
     *
     * @return The order, zero by default
     */
    @Override
    default int getOrder() {
        return 0;
    }

    /**
     * Resolves an instance or static method, or a constructor represented by {@code <init>}.
     *
     * @param context       The EL context
     * @param base          The instance, or an {@link ELClass} for a static method or constructor
     * @param method        The method name
     * @param argumentTypes The parameter types supplied when the method expression was created, or {@code null}
     * @param arguments     The evaluated arguments, or {@code null}
     * @return The resolved method, or {@code null} when this executor does not handle it
     */
    @Nullable
    ELMethod resolve(ELContext context,
                     @Nullable Object base,
                     @Nullable Object method,
                     Argument<?> @Nullable [] argumentTypes,
                     Object @Nullable [] arguments);

    /**
     * Resolves a function bound by the expression context.
     *
     * <p>The default implementation does not contribute functions. A reflection-backed implementation can use
     * the Jakarta {@link jakarta.el.FunctionMapper} here, while a generated implementation can provide functions
     * without exposing reflective methods.</p>
     *
     * @param context   The EL context
     * @param prefix    The function prefix
     * @param localName The function name
     * @return The resolved function, or {@code null} when this executor does not handle it
     */
    @Nullable
    default ELMethod resolveFunction(ELContext context, String prefix, String localName) {
        return null;
    }
}
