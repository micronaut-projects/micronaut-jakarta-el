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
package io.micronaut.el.parser;

import io.micronaut.core.annotation.Internal;

/**
 * A token of a Jakarta Expression Language expression.
 *
 * @param type     The type of the token
 * @param value    The value of the token
 * @param position The position of the token in the expression
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
record Token(TokenType type, String value, int position) {

    /**
     * @param expected The expected type
     * @return True when the token is of the expected type
     */
    public boolean is(TokenType expected) {
        return type == expected;
    }

    @Override
    public String toString() {
        return type + "('" + value + "')";
    }
}
