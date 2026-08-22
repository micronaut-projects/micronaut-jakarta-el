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

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.Map;

/**
 * The tokenizer of the Jakarta Expression Language grammar collected in the section 1.26 of the
 * specification.
 *
 * <p>The tokenizer reproduces the lexical states of the grammar: the text outside of an eval-expression is
 * returned as a single token, and the nesting of the set and map constructions is tracked so that the
 * closing brace of an eval-expression is recognized.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELTokenizer {

    private static final Map<String, TokenType> KEYWORDS = Map.ofEntries(
        Map.entry("true", TokenType.TRUE),
        Map.entry("false", TokenType.FALSE),
        Map.entry("null", TokenType.NULL),
        Map.entry("empty", TokenType.EMPTY),
        Map.entry("instanceof", TokenType.INSTANCEOF),
        Map.entry("not", TokenType.NOT),
        Map.entry("and", TokenType.AND),
        Map.entry("or", TokenType.OR),
        Map.entry("div", TokenType.DIV),
        Map.entry("mod", TokenType.MOD),
        Map.entry("eq", TokenType.EQ),
        Map.entry("ne", TokenType.NE),
        Map.entry("lt", TokenType.LT),
        Map.entry("gt", TokenType.GT),
        Map.entry("le", TokenType.LE),
        Map.entry("ge", TokenType.GE)
    );

    private final String expression;
    private final List<Token> tokens = new ArrayList<>();
    private final Deque<Boolean> states = new ArrayDeque<>();
    private boolean inExpression;
    private int position;

    private ELTokenizer(String expression) {
        this.expression = expression;
    }

    /**
     * Tokenizes an expression.
     *
     * @param expression The expression
     * @return The tokens, always terminated by a token of the type {@link TokenType#EOF}
     */
    public static List<Token> tokenize(String expression) {
        return new ELTokenizer(expression).tokenize();
    }

    private List<Token> tokenize() {
        while (position < expression.length()) {
            if (inExpression) {
                readExpressionToken();
            } else {
                readLiteralText();
            }
        }
        if (inExpression) {
            throw error("Unterminated eval-expression");
        }
        tokens.add(new Token(TokenType.EOF, "", position));
        return tokens;
    }

    private void readLiteralText() {
        StringBuilder text = new StringBuilder();
        int start = position;
        while (position < expression.length()) {
            char c = expression.charAt(position);
            if (c == '\\' && position + 1 < expression.length()) {
                char next = expression.charAt(position + 1);
                if (next == '\\' || next == '$' || next == '#') {
                    text.append(next);
                    position += 2;
                    continue;
                }
            }
            if ((c == '$' || c == '#') && position + 1 < expression.length()
                && expression.charAt(position + 1) == '{') {
                break;
            }
            text.append(c);
            position++;
        }
        if (!text.isEmpty()) {
            tokens.add(new Token(TokenType.LITERAL_TEXT, text.toString(), start));
        }
        if (position < expression.length()) {
            char c = expression.charAt(position);
            tokens.add(new Token(c == '$' ? TokenType.START_DYNAMIC : TokenType.START_DEFERRED,
                expression.substring(position, position + 2), position));
            position += 2;
            states.push(Boolean.FALSE);
            inExpression = true;
        }
    }

    @SuppressWarnings("java:S3776")
    private void readExpressionToken() {
        char c = expression.charAt(position);
        if (Character.isWhitespace(c)) {
            position++;
            return;
        }
        if (c == '{') {
            add(TokenType.START_MAP, "{", 1);
            states.push(Boolean.TRUE);
            return;
        }
        if (c == '}') {
            add(TokenType.RCURL, "}", 1);
            inExpression = Boolean.TRUE.equals(states.pop());
            return;
        }
        if (c == '\'' || c == '"') {
            readString(c);
            return;
        }
        if (Character.isDigit(c) || c == '.' && position + 1 < expression.length()
            && Character.isDigit(expression.charAt(position + 1))) {
            readNumber();
            return;
        }
        if (isIdentifierStart(c)) {
            readIdentifier();
            return;
        }
        readOperator();
    }

    private void readString(char quote) {
        int start = position;
        StringBuilder value = new StringBuilder();
        position++;
        while (position < expression.length()) {
            char c = expression.charAt(position);
            if (c == '\\') {
                if (position + 1 >= expression.length()) {
                    throw error("Unterminated string literal");
                }
                char next = expression.charAt(position + 1);
                if (next != '\\' && next != '\'' && next != '"') {
                    throw error("Invalid escape sequence '\\" + next + "' in a string literal");
                }
                value.append(next);
                position += 2;
                continue;
            }
            if (c == quote) {
                position++;
                tokens.add(new Token(TokenType.STRING_LITERAL, value.toString(), start));
                return;
            }
            value.append(c);
            position++;
        }
        throw error("Unterminated string literal");
    }

    private void readNumber() {
        int start = position;
        boolean floatingPoint = false;
        while (position < expression.length() && Character.isDigit(expression.charAt(position))) {
            position++;
        }
        if (position < expression.length() && expression.charAt(position) == '.') {
            floatingPoint = true;
            position++;
            while (position < expression.length() && Character.isDigit(expression.charAt(position))) {
                position++;
            }
        }
        if (position < expression.length()
            && (expression.charAt(position) == 'e' || expression.charAt(position) == 'E')) {
            int exponent = position + 1;
            if (exponent < expression.length()
                && (expression.charAt(exponent) == '+' || expression.charAt(exponent) == '-')) {
                exponent++;
            }
            if (exponent < expression.length() && Character.isDigit(expression.charAt(exponent))) {
                floatingPoint = true;
                position = exponent;
                while (position < expression.length() && Character.isDigit(expression.charAt(position))) {
                    position++;
                }
            }
        }
        String value = expression.substring(start, position);
        tokens.add(new Token(floatingPoint ? TokenType.FLOATING_POINT_LITERAL : TokenType.INTEGER_LITERAL,
            value, start));
    }

    private void readIdentifier() {
        int start = position;
        position++;
        while (position < expression.length() && isIdentifierPart(expression.charAt(position))) {
            position++;
        }
        String value = expression.substring(start, position);
        TokenType keyword = KEYWORDS.get(value);
        tokens.add(new Token(keyword == null ? TokenType.IDENTIFIER : keyword, value, start));
    }

    @SuppressWarnings("java:S3776")
    private void readOperator() {
        char c = expression.charAt(position);
        char next = position + 1 < expression.length() ? expression.charAt(position + 1) : '\0';
        switch (c) {
            case '.' -> add(TokenType.DOT, ".", 1);
            case '(' -> add(TokenType.LPAREN, "(", 1);
            case ')' -> add(TokenType.RPAREN, ")", 1);
            case '[' -> add(TokenType.LBRACK, "[", 1);
            case ']' -> add(TokenType.RBRACK, "]", 1);
            case ':' -> add(TokenType.COLON, ":", 1);
            case ',' -> add(TokenType.COMMA, ",", 1);
            case ';' -> add(TokenType.SEMICOLON, ";", 1);
            case '?' -> add(TokenType.QUESTIONMARK, "?", 1);
            case '*' -> add(TokenType.MULT, "*", 1);
            case '/' -> add(TokenType.DIV, "/", 1);
            case '%' -> add(TokenType.MOD, "%", 1);
            case '+' -> {
                if (next == '=') {
                    add(TokenType.CONCAT, "+=", 2);
                } else {
                    add(TokenType.PLUS, "+", 1);
                }
            }
            case '-' -> {
                if (next == '>') {
                    add(TokenType.ARROW, "->", 2);
                } else {
                    add(TokenType.MINUS, "-", 1);
                }
            }
            case '=' -> {
                if (next == '=') {
                    add(TokenType.EQ, "==", 2);
                } else {
                    add(TokenType.ASSIGN, "=", 1);
                }
            }
            case '!' -> {
                if (next == '=') {
                    add(TokenType.NE, "!=", 2);
                } else {
                    add(TokenType.NOT, "!", 1);
                }
            }
            case '<' -> {
                if (next == '=') {
                    add(TokenType.LE, "<=", 2);
                } else {
                    add(TokenType.LT, "<", 1);
                }
            }
            case '>' -> {
                if (next == '=') {
                    add(TokenType.GE, ">=", 2);
                } else {
                    add(TokenType.GT, ">", 1);
                }
            }
            case '&' -> {
                if (next == '&') {
                    add(TokenType.AND, "&&", 2);
                } else {
                    throw error("Unexpected character '&'");
                }
            }
            case '|' -> {
                if (next == '|') {
                    add(TokenType.OR, "||", 2);
                } else {
                    throw error("Unexpected character '|'");
                }
            }
            default -> throw error("Unexpected character '" + c + "'");
        }
    }

    private void add(TokenType type, String value, int length) {
        tokens.add(new Token(type, value, position));
        position += length;
    }

    private static boolean isIdentifierStart(char c) {
        return Character.isJavaIdentifierStart(c) || c == '#';
    }

    private static boolean isIdentifierPart(char c) {
        return Character.isJavaIdentifierPart(c);
    }

    private ELParsingException error(String message) {
        return new ELParsingException(message, expression, position);
    }
}
