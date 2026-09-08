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
import io.micronaut.core.annotation.Internal;
import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

/**
 * Implements a functional interface nothing described in advance, which is the last resort of the coercion of
 * the section 1.23.2 of the specification.
 *
 * <p>The coercion needs the single abstract method of the target interface. The functional interfaces of the
 * platform are implemented by this module directly, and an application declares its own to
 * {@link ELMethodRegistry#functionalInterface}, so neither reaches here. What does is an interface no one
 * named, which can only be implemented by discovering its method at runtime: that is reflection, so it is
 * contributed by the interpreter-reflection module rather than performed here.</p>
 *
 * <p>An application implements {@link ELMethodContributor} rather than this. Without a factory on the
 * classpath the coercion does not apply, and a lambda expression reaching a parameter of an undescribed
 * functional interface type is reported as a failed coercion.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
@Experimental
public interface ELProxyFactory {

    /**
     * Whether the given type is a functional interface this factory can implement.
     *
     * @param type The target type
     * @return Whether the type is a functional interface
     */
    boolean isFunctionalInterface(Class<?> type);

    /**
     * Implements the functional interface with the lambda expression.
     *
     * @param context The context, or {@code null} when the lambda carries its own
     * @param lambda  The lambda expression
     * @param type    The functional interface
     * @param <T>     The functional interface
     * @return The instance
     */
    <T> T create(@Nullable ELContext context, LambdaExpression lambda, Class<T> type);
}
