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
import io.micronaut.core.type.Argument;
import org.jspecify.annotations.Nullable;

import java.util.Arrays;

/**
 * Adapts the raw parameter types of the Jakarta EL API to Micronaut arguments.
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Internal
public final class ELArguments {

    private ELArguments() {
    }

    /**
     * Returns Micronaut arguments for raw Jakarta EL parameter types.
     *
     * @param types Raw parameter types
     * @return The corresponding arguments, or {@code null}
     */
    public static Argument<?> @Nullable [] of(Class<?> @Nullable [] types) {
        return types == null ? null : Arrays.stream(types).map(Argument::of).toArray(Argument<?>[]::new);
    }
}
