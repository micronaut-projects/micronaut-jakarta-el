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

import jakarta.el.ELException;

/**
 * An exception raised when an expression cannot be parsed.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public final class ELParsingException extends ELException {

    private static final long serialVersionUID = 1L;

    /**
     * @param message    The message
     * @param expression The expression
     * @param position   The position of the error
     */
    public ELParsingException(String message, String expression, int position) {
        super(message + " in the expression [" + expression + "] at the position " + position);
    }

    /**
     * @param message The message
     */
    public ELParsingException(String message) {
        super(message);
    }
}
