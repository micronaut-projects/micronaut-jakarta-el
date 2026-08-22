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

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;

/**
 * The compiled body of a Jakarta Expression Language lambda expression.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@FunctionalInterface
@Internal
public interface ELLambdaBody {

    /**
     * Evaluates the body of the lambda expression. The lambda parameters are available as lambda arguments
     * of the given context.
     *
     * @param context The context
     * @return The result of the evaluation
     */
    @Nullable
    Object evaluate(ELContext context);
}
