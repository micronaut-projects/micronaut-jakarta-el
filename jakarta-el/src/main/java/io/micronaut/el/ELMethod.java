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
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;

/**
 * A method resolved by an {@link ELMethodExecutor}.
 *
 * <p>The descriptor contains the metadata exposed by {@link jakarta.el.MethodExpression} and the invocation
 * itself. It deliberately has no {@code java.lang.reflect} dependency, so a service can implement it with a
 * generated dispatch method or any other direct invocation mechanism.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Experimental
public interface ELMethod extends Serializable {

    /**
     * @return The method name
     */
    String getName();

    /**
     * @return The return type
     */
    Class<?> getReturnType();

    /**
     * @return The declared parameter types
     */
    Class<?>[] getParameterTypes();

    /**
     * @return Whether the method has a variable arity parameter
     */
    boolean isVarArgs();

    /**
     * @return The method annotations, or an empty array when there are none
     */
    default Annotation[] getAnnotations() {
        return new Annotation[0];
    }

    /**
     * Invokes the method.
     *
     * @param context   The EL context
     * @param base      The instance, or {@code null} for a static method
     * @param arguments The arguments
     * @return The result
     */
    @Nullable
    Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments);

    /**
     * Returns the identity used when an expression containing a bound function is compared with another one.
     * Implementations with overloads should include the declaring type in the identity.
     *
     * @return The method identity
     */
    default String identity() {
        return getClass().getName() + '#' + getName() + java.util.Arrays.toString(getParameterTypes());
    }
}
