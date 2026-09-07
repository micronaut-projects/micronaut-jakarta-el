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
package io.micronaut.el.tck;

import com.sun.ts.tests.el.spec.coercion.ELClientIT;
import jakarta.el.ELException;

import java.util.function.Predicate;

/**
 * Compile-time function declarations for functions installed dynamically by the TCK.
 */
public final class TckFunctions {

    private TckFunctions() {
    }

    /**
     * Implements the function the TCK maps to {@link Integer#valueOf(String)}.
     *
     * @param value The value
     * @return The integer
     */
    public static Integer val(String value) {
        try {
            return Integer.valueOf(value);
        } catch (NumberFormatException e) {
            // A FunctionMapper invocation exposes an exception from the mapped method through ELException.
            throw new ELException(e);
        }
    }

    /**
     * @param value The array
     * @return The result of the TCK function
     */
    public static int testPrimitiveBooleanArray(boolean[] value) {
        return ELClientIT.testPrimitiveBooleanArray(value);
    }

    /**
     * @param predicate The predicate
     * @return The result of the TCK function
     */
    public static String testPredicateString(Predicate<String> predicate) {
        return ELClientIT.testPredicateString(predicate);
    }

    /**
     * @param predicate The predicate
     * @return The result of the TCK function
     */
    public static String testPredicateLong(Predicate<Long> predicate) {
        return ELClientIT.testPredicateLong(predicate);
    }
}
