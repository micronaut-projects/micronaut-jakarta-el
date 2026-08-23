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
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

/**
 * The compiled body of a Jakarta Expression Language lambda expression.
 *
 * <p>The parameters of the lambda expression are the parameters of the body, so that the generated code reads
 * them as Java locals rather than through {@link ELContext#getLambdaArgument(String)}. The nested interfaces
 * are the forms the generated code implements, one per arity up to three, the general form takes the arguments
 * as an array.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@FunctionalInterface
@Internal
public interface ELLambdaBody {

    /**
     * Evaluates the body of the lambda expression.
     *
     * @param context   The context the lambda expression is invoked with
     * @param arguments The arguments, at least as many as the lambda expression declares parameters
     * @return The result of the evaluation
     */
    @Nullable
    Object evaluate(ELContext context, @Nullable Object[] arguments);

    /**
     * The body of a lambda expression without parameters.
     */
    @FunctionalInterface
    interface Nullary extends ELLambdaBody {

        /**
         * @param context The context
         * @return The result of the evaluation
         */
        @Nullable
        Object evaluate(ELContext context);

        @Override
        @Nullable
        default Object evaluate(ELContext context, @Nullable Object[] arguments) {
            return evaluate(context);
        }
    }

    /**
     * The body of a lambda expression with one parameter.
     */
    @FunctionalInterface
    interface Unary extends ELLambdaBody {

        /**
         * @param context The context
         * @param first   The first argument
         * @return The result of the evaluation
         */
        @Nullable
        Object evaluate(ELContext context, @Nullable Object first);

        @Override
        @Nullable
        default Object evaluate(ELContext context, @Nullable Object[] arguments) {
            return evaluate(context, arguments[0]);
        }
    }

    /**
     * The body of a lambda expression with two parameters.
     */
    @FunctionalInterface
    interface Binary extends ELLambdaBody {

        /**
         * @param context The context
         * @param first   The first argument
         * @param second  The second argument
         * @return The result of the evaluation
         */
        @Nullable
        Object evaluate(ELContext context, @Nullable Object first, @Nullable Object second);

        @Override
        @Nullable
        default Object evaluate(ELContext context, @Nullable Object[] arguments) {
            return evaluate(context, arguments[0], arguments[1]);
        }
    }

    /**
     * The body of a lambda expression with three parameters.
     */
    @FunctionalInterface
    interface Ternary extends ELLambdaBody {

        /**
         * @param context The context
         * @param first   The first argument
         * @param second  The second argument
         * @param third   The third argument
         * @return The result of the evaluation
         */
        @Nullable
        Object evaluate(ELContext context, @Nullable Object first, @Nullable Object second, @Nullable Object third);

        @Override
        @Nullable
        default Object evaluate(ELContext context, @Nullable Object[] arguments) {
            return evaluate(context, arguments[0], arguments[1], arguments[2]);
        }
    }
}
