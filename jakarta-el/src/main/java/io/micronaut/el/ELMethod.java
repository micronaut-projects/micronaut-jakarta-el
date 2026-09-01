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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.type.Argument;
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
     * Returns the method name.
     *
     * @return The method name
     */
    String getName();

    /**
     * Returns the return type.
     *
     * @return The return type
     */
    Argument<?> getReturnType();

    /**
     * Returns the declared parameter types.
     *
     * @return The declared parameter types
     */
    Argument<?>[] getArguments();

    /**
     * Returns whether the method has a variable arity parameter.
     *
     * @return Whether the method has a variable arity parameter
     */
    boolean isVarArgs();

    /**
     * Returns the method annotation metadata.
     *
     * @return The method annotation metadata, or empty metadata when there is none
     */
    default AnnotationMetadata getAnnotationMetadata() {
        return AnnotationMetadata.EMPTY_METADATA;
    }

    /**
     * Synthesizes the annotations required by the Jakarta EL method-reference API.
     *
     * @return The method annotations
     */
    default Annotation[] synthesizeAnnotations() {
        return getAnnotationMetadata().synthesizeAll();
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
     * Whether this method can be kept by the caller and invoked again.
     *
     * <p>A reusable method holds its signature and the code that runs it, and nothing of the call it was
     * resolved for: the interpreter may then resolve a call site once and invoke the same method on every
     * later evaluation, instead of resolving it again each time. A method that captured the arguments it was
     * resolved with, or that only applies to their runtime types, is not reusable.</p>
     *
     * <p>The default is {@code false}, so an implementation is resolved again per call until it states
     * otherwise. A method of an overloaded name is only reusable when the arguments cannot select a different
     * overload.</p>
     *
     * @return Whether the method can be invoked again with other arguments
     */
    default boolean isReusable() {
        return false;
    }

    /**
     * Returns the identity used when an expression containing a bound function is compared with another one.
     * Implementations with overloads should include the declaring type in the identity.
     *
     * @return The method identity
     * @see #identity(Class, String, Class[])
     */
    default String identity() {
        return getClass().getName() + '#' + getName()
            + java.util.Arrays.toString(Argument.toClassArray(getArguments()));
    }

    /**
     * Builds the identity of a method in the form the compiler gives it, so that an expression parsed at
     * runtime compares equal to the same expression compiled at build time.
     *
     * @param declaringType  The type declaring the method
     * @param name           The name of the method
     * @param parameterTypes The declared parameter types
     * @return The method identity
     */
    static String identity(Class<?> declaringType, String name, Class<?>[] parameterTypes) {
        StringBuilder identity = new StringBuilder(declaringType.getName()).append('#').append(name).append('(');
        for (int i = 0; i < parameterTypes.length; i++) {
            if (i > 0) {
                identity.append(',');
            }
            identity.append(parameterTypes[i].getName());
        }
        return identity.append(')').toString();
    }
}
