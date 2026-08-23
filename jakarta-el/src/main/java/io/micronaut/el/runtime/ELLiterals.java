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

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The handling of the literal-expressions described in the section 1.2.2 of the Jakarta Expression
 * Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELLiterals {

    private ELLiterals() {
    }

    /**
     * Returns the text of a literal-expression, with the {@code \$} and {@code \#} escapes resolved.
     *
     * @param expression The expression
     * @return The literal text or {@code null} when the expression contains an eval-expression
     */
    @Nullable
    public static String literalTextOrNull(String expression) {
        StringBuilder text = new StringBuilder(expression.length());
        int length = expression.length();
        for (int i = 0; i < length; i++) {
            char c = expression.charAt(i);
            if (c == '\\' && i + 1 < length) {
                char next = expression.charAt(i + 1);
                if (next == '\\' || next == '$' || next == '#') {
                    text.append(next);
                    i++;
                    continue;
                }
            }
            if ((c == '$' || c == '#') && i + 1 < length && expression.charAt(i + 1) == '{') {
                return null;
            }
            text.append(c);
        }
        return text.toString();
    }

    /**
     * Returns the value of an integer literal, which is a {@link Long} unless it does not fit in one.
     *
     * @param image The literal as it appears in the expression
     * @return The value of the literal
     */
    public static Number integerValue(String image) {
        try {
            return Long.valueOf(image);
        } catch (NumberFormatException e) {
            return new BigInteger(image);
        }
    }

    /**
     * Returns the value of a floating point literal, which is a {@link Double} unless it does not fit in one.
     *
     * @param image The literal as it appears in the expression
     * @return The value of the literal
     */
    public static Number floatingPointValue(String image) {
        double value = Double.parseDouble(image);
        if (Double.isInfinite(value)) {
            return new BigDecimal(image);
        }
        return value;
    }
}
