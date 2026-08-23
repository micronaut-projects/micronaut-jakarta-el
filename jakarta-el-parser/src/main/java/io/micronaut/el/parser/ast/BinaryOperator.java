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
package io.micronaut.el.parser.ast;

import io.micronaut.core.annotation.Experimental;


/**
 * The binary operators of the Jakarta Expression Language.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public enum BinaryOperator {

    ADD,
    SUBTRACT,
    MULTIPLY,
    DIVIDE,
    MODULO,
    /**
     * The string concatenation operator of the section 1.8 of the specification.
     */
    CONCAT,
    LESS_THAN,
    GREATER_THAN,
    LESS_THAN_OR_EQUAL,
    GREATER_THAN_OR_EQUAL,
    EQUAL,
    NOT_EQUAL,
    AND,
    OR
}
