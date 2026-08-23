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
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The abstract syntax tree of a parsed Jakarta Expression Language expression.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public sealed interface ELNode {

    /**
     * @return The kind of the node, which an evaluator switches on: a switch on an enum is a table switch,
     * where a pattern switch over the sealed types is a linear classification through a method handle
     */
    Kind kind();

    /**
     * The kinds of nodes, one per record.
     */
    enum Kind {
        /** Composite. */
        COMPOSITE,
        /** LiteralText. */
        LITERAL_TEXT,
        /** Eval. */
        EVAL,
        /** NullLiteral. */
        NULL_LITERAL,
        /** BooleanLiteral. */
        BOOLEAN_LITERAL,
        /** IntegerLiteral. */
        INTEGER_LITERAL,
        /** FloatingPointLiteral. */
        FLOATING_POINT_LITERAL,
        /** StringLiteral. */
        STRING_LITERAL,
        /** Identifier. */
        IDENTIFIER,
        /** Function. */
        FUNCTION,
        /** Property. */
        PROPERTY,
        /** Method. */
        METHOD,
        /** Call. */
        CALL,
        /** Unary. */
        UNARY,
        /** Binary. */
        BINARY,
        /** Ternary. */
        TERNARY,
        /** Assign. */
        ASSIGN,
        /** Semicolon. */
        SEMICOLON,
        /** Lambda. */
        LAMBDA,
        /** SetData. */
        SET_DATA,
        /** ListData. */
        LIST_DATA,
        /** MapData. */
        MAP_DATA
    }

    /**
     * A composite expression, as described in the section 1.2.3 of the specification.
     *
     * @param parts The literal-expressions and the eval-expressions
     */
    record Composite(List<ELNode> parts) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.COMPOSITE;
        }
    }

    /**
     * A literal-expression, as described in the section 1.2.2 of the specification.
     *
     * @param text The text of the expression
     */
    record LiteralText(String text) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.LITERAL_TEXT;
        }
    }

    /**
     * An eval-expression, as described in the section 1.2.1 of the specification.
     *
     * @param expression The expression
     */
    record Eval(ELNode expression) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.EVAL;
        }
    }

    /**
     * The {@code null} literal.
     */
    record NullLiteral() implements ELNode {
        @Override
        public Kind kind() {
            return Kind.NULL_LITERAL;
        }
    }

    /**
     * A boolean literal.
     *
     * @param value The value
     */
    record BooleanLiteral(boolean value) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.BOOLEAN_LITERAL;
        }
    }

    /**
     * An integer literal.
     *
     * @param image The literal as it appears in the expression
     * @param value The value of the literal, a {@link Long} unless it does not fit in one, then a
     *              {@link java.math.BigInteger}, computed once when the expression is parsed
     */
    record IntegerLiteral(String image, Number value) implements ELNode {

        /**
         * @param image The literal as it appears in the expression
         */
        public IntegerLiteral(String image) {
            this(image, integerValue(image));
        }

        private static Number integerValue(String image) {
            try {
                return Long.valueOf(image);
            } catch (NumberFormatException e) {
                return new java.math.BigInteger(image);
            }
        }

        @Override
        public Kind kind() {
            return Kind.INTEGER_LITERAL;
        }
    }

    /**
     * A floating point literal.
     *
     * @param image The literal as it appears in the expression
     * @param value The value of the literal, a {@link Double} unless it does not fit in one, then a
     *              {@link java.math.BigDecimal}, computed once when the expression is parsed
     */
    record FloatingPointLiteral(String image, Number value) implements ELNode {

        /**
         * @param image The literal as it appears in the expression
         */
        public FloatingPointLiteral(String image) {
            this(image, floatingPointValue(image));
        }

        private static Number floatingPointValue(String image) {
            double value = Double.parseDouble(image);
            return Double.isInfinite(value) ? new java.math.BigDecimal(image) : Double.valueOf(value);
        }

        @Override
        public Kind kind() {
            return Kind.FLOATING_POINT_LITERAL;
        }
    }

    /**
     * A string literal.
     *
     * @param value The value, with the escape sequences resolved
     */
    record StringLiteral(String value) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.STRING_LITERAL;
        }
    }

    /**
     * An identifier, resolved as described in the section 1.5.1 of the specification.
     *
     * @param name The name
     */
    record Identifier(String name) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.IDENTIFIER;
        }
    }

    /**
     * A function invocation, resolved as described in the section 1.5.2 of the specification.
     *
     * @param prefix      The namespace prefix, empty when the function has no namespace
     * @param localName   The local name
     * @param invocations The argument lists of the chained invocations
     */
    record Function(String prefix,
                    String localName,
                    List<List<ELNode>> invocations) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.FUNCTION;
        }
    }

    /**
     * A property resolution with the {@code .} or the {@code []} operator.
     *
     * @param base     The base expression
     * @param property The property expression
     */
    record Property(ELNode base, ELNode property) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.PROPERTY;
        }
    }

    /**
     * A method invocation with the {@code .} or the {@code []} operator.
     *
     * @param base      The base expression
     * @param property  The method name expression
     * @param arguments The arguments
     */
    record Method(ELNode base,
                  ELNode property,
                  List<ELNode> arguments) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.METHOD;
        }
    }

    /**
     * The invocation of the value of an expression, such as a lambda expression.
     *
     * @param target    The invoked expression
     * @param arguments The arguments
     */
    record Call(ELNode target, List<ELNode> arguments) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.CALL;
        }
    }

    /**
     * A unary operation.
     *
     * @param operator The operator
     * @param operand  The operand
     */
    record Unary(UnaryOperator operator, ELNode operand) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.UNARY;
        }
    }

    /**
     * A binary operation.
     *
     * @param operator The operator
     * @param left     The left operand
     * @param right    The right operand
     */
    record Binary(BinaryOperator operator,
                  ELNode left,
                  ELNode right) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.BINARY;
        }
    }

    /**
     * The conditional operator of the section 1.12 of the specification.
     *
     * @param condition The condition
     * @param ifTrue    The expression evaluated when the condition is true
     * @param ifFalse   The expression evaluated when the condition is false
     */
    record Ternary(ELNode condition,
                   ELNode ifTrue,
                   ELNode ifFalse) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.TERNARY;
        }
    }

    /**
     * The assignment operator of the section 1.13 of the specification.
     *
     * @param target The lvalue
     * @param value  The assigned expression
     */
    record Assign(ELNode target, ELNode value) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.ASSIGN;
        }
    }

    /**
     * The semicolon operator of the section 1.14 of the specification.
     *
     * @param left  The discarded expression
     * @param right The returned expression
     */
    record Semicolon(ELNode left, ELNode right) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.SEMICOLON;
        }
    }

    /**
     * A lambda expression, as described in the section 1.20 of the specification.
     *
     * @param parameters The formal parameters
     * @param body       The body
     */
    record Lambda(List<String> parameters, ELNode body) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.LAMBDA;
        }
    }

    /**
     * A set construction, as described in the section 2.2.1 of the specification.
     *
     * @param elements The elements
     */
    record SetData(List<ELNode> elements) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.SET_DATA;
        }
    }

    /**
     * A list construction, as described in the section 2.2.2 of the specification.
     *
     * @param elements The elements
     */
    record ListData(List<ELNode> elements) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.LIST_DATA;
        }
    }

    /**
     * A map construction, as described in the section 2.2.3 of the specification.
     *
     * @param entries The entries
     */
    record MapData(List<MapEntry> entries) implements ELNode {
        @Override
        public Kind kind() {
            return Kind.MAP_DATA;
        }

        /**
         * An entry of a map construction.
         *
         * @param key   The key
         * @param value The value
         */
        public record MapEntry(ELNode key, @Nullable ELNode value) {
        }
    }
}
