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
import org.jspecify.annotations.Nullable;

/**
 * The token types of the Jakarta Expression Language grammar, as collected in the section 1.26 of the
 * specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
enum TokenType {

    /**
     * The text outside of an eval-expression.
     */
    LITERAL_TEXT(null),
    /**
     * The start of a {@code ${...}} expression.
     */
    START_DYNAMIC("${"),
    /**
     * The start of a {@code #{...}} expression.
     */
    START_DEFERRED("#{"),
    /**
     * The start of a set or map construction.
     */
    START_MAP("{"),
    /**
     * The end of an expression, of a set construction or of a map construction.
     */
    RCURL("}"),

    INTEGER_LITERAL(null),
    FLOATING_POINT_LITERAL(null),
    STRING_LITERAL(null),
    IDENTIFIER(null),

    TRUE("true"),
    FALSE("false"),
    NULL("null"),
    EMPTY("empty"),
    INSTANCEOF("instanceof"),
    NOT("not"),
    AND("and"),
    OR("or"),
    DIV("div"),
    MOD("mod"),
    EQ("eq"),
    NE("ne"),
    LT("lt"),
    GT("gt"),
    LE("le"),
    GE("ge"),

    DOT("."),
    LPAREN("("),
    RPAREN(")"),
    LBRACK("["),
    RBRACK("]"),
    COLON(":"),
    COMMA(","),
    SEMICOLON(";"),
    QUESTIONMARK("?"),
    MULT("*"),
    PLUS("+"),
    MINUS("-"),
    CONCAT("+="),
    ASSIGN("="),
    ARROW("->"),

    /**
     * The end of the input.
     */
    EOF(null);

    private final @Nullable String symbol;

    TokenType(@Nullable String symbol) {
        this.symbol = symbol;
    }

    /**
     * @return The symbol of the token, {@code null} when the token has no fixed representation
     */
    @Nullable
    public String getSymbol() {
        return symbol;
    }
}
