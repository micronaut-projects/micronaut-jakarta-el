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
package io.micronaut.el.example.eligible;

/**
 * How the remappers store the text of an expression in the annotation metadata.
 */
final class ExpressionTexts {

    private ExpressionTexts() {
    }

    /**
     * The text with every {@code ${...}} stored as {@code #{...}}: the specification treats the two alike, while
     * Micronaut resolves a {@code ${...}} in the metadata of a bean as a property placeholder. Every one is
     * rewritten, not only a leading one, since a composite expression holds several; an escaped
     * {@code \${}, which is literal text to the specification, is kept as it is.
     *
     * @param text The text as written
     * @return The text as stored
     */
    static String deferred(String text) {
        StringBuilder stored = new StringBuilder(text.length());
        int i = 0;
        while (i < text.length()) {
            char c = text.charAt(i);
            char next = i + 1 < text.length() ? text.charAt(i + 1) : 0;
            if (c == '\\' && (next == '$' || next == '#')) {
                // an escaped delimiter: the backslash and the character it escapes stay literal text
                stored.append(c).append(next);
                i += 2;
            } else {
                stored.append(c == '$' && next == '{' ? '#' : c);
                i++;
            }
        }
        return stored.toString();
    }
}
