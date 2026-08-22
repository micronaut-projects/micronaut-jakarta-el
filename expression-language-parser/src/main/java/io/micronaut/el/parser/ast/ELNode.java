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

import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The abstract syntax tree of a parsed Jakarta Expression Language expression.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public sealed interface ELNode {

    /**
     * A composite expression, as described in the section 1.2.3 of the specification.
     *
     * @param parts The literal-expressions and the eval-expressions
     */
    record Composite(List<ELNode> parts) implements ELNode {
    }

    /**
     * A literal-expression, as described in the section 1.2.2 of the specification.
     *
     * @param text The text of the expression
     */
    record LiteralText(String text) implements ELNode {
    }

    /**
     * An eval-expression, as described in the section 1.2.1 of the specification.
     *
     * @param expression The expression
     */
    record Eval(ELNode expression) implements ELNode {
    }

    /**
     * The {@code null} literal.
     */
    record NullLiteral() implements ELNode {
    }

    /**
     * A boolean literal.
     *
     * @param value The value
     */
    record BooleanLiteral(boolean value) implements ELNode {
    }

    /**
     * An integer literal.
     *
     * @param image The literal as it appears in the expression
     */
    record IntegerLiteral(String image) implements ELNode {
    }

    /**
     * A floating point literal.
     *
     * @param image The literal as it appears in the expression
     */
    record FloatingPointLiteral(String image) implements ELNode {
    }

    /**
     * A string literal.
     *
     * @param value The value, with the escape sequences resolved
     */
    record StringLiteral(String value) implements ELNode {
    }

    /**
     * An identifier, resolved as described in the section 1.5.1 of the specification.
     *
     * @param name The name
     */
    record Identifier(String name) implements ELNode {
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
    }

    /**
     * A property resolution with the {@code .} or the {@code []} operator.
     *
     * @param base     The base expression
     * @param property The property expression
     */
    record Property(ELNode base, ELNode property) implements ELNode {
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
    }

    /**
     * The invocation of the value of an expression, such as a lambda expression.
     *
     * @param target    The invoked expression
     * @param arguments The arguments
     */
    record Call(ELNode target, List<ELNode> arguments) implements ELNode {
    }

    /**
     * A unary operation.
     *
     * @param operator The operator
     * @param operand  The operand
     */
    record Unary(UnaryOperator operator, ELNode operand) implements ELNode {
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
    }

    /**
     * The assignment operator of the section 1.13 of the specification.
     *
     * @param target The lvalue
     * @param value  The assigned expression
     */
    record Assign(ELNode target, ELNode value) implements ELNode {
    }

    /**
     * The semicolon operator of the section 1.14 of the specification.
     *
     * @param left  The discarded expression
     * @param right The returned expression
     */
    record Semicolon(ELNode left, ELNode right) implements ELNode {
    }

    /**
     * A lambda expression, as described in the section 1.20 of the specification.
     *
     * @param parameters The formal parameters
     * @param body       The body
     */
    record Lambda(List<String> parameters, ELNode body) implements ELNode {
    }

    /**
     * A set construction, as described in the section 2.2.1 of the specification.
     *
     * @param elements The elements
     */
    record SetData(List<ELNode> elements) implements ELNode {
    }

    /**
     * A list construction, as described in the section 2.2.2 of the specification.
     *
     * @param elements The elements
     */
    record ListData(List<ELNode> elements) implements ELNode {
    }

    /**
     * A map construction, as described in the section 2.2.3 of the specification.
     *
     * @param entries The entries
     */
    record MapData(List<MapEntry> entries) implements ELNode {

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
