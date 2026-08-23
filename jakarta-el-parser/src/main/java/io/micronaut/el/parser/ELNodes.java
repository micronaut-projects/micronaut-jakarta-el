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

import io.micronaut.core.annotation.Experimental;
import io.micronaut.el.parser.ast.BinaryOperator;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.parser.ast.UnaryOperator;

import java.util.List;

/**
 * Utilities over the parsed form of an expression.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class ELNodes {

    private ELNodes() {
    }

    /**
     * Renders a parsed expression in a canonical form, so that two expressions that differ only in whitespace,
     * in the delimiters or in the spelling of an operator render identically.
     *
     * <p>The canonical form is what {@code jakarta.el.Expression.equals} compares, which the specification
     * defines as representing the same expression "taking whitespace and operator aliases into account". Every
     * sub-expression is parenthesised, so precedence never has to be reconstructed.</p>
     *
     * @param node The parsed expression
     * @return The canonical form
     */
    public static String canonical(ELNode node) {
        StringBuilder builder = new StringBuilder();
        append(builder, node);
        return builder.toString();
    }

    @SuppressWarnings("java:S1541")
    private static void append(StringBuilder out, ELNode node) {
        switch (node) {
            case ELNode.Composite composite -> composite.parts().forEach(part -> append(out, part));
            case ELNode.LiteralText literal -> appendLiteralText(out, literal.text());
            case ELNode.Eval eval -> {
                out.append("${");
                append(out, eval.expression());
                out.append('}');
            }
            case ELNode.NullLiteral ignored -> out.append("null");
            case ELNode.BooleanLiteral literal -> out.append(literal.value());
            case ELNode.IntegerLiteral literal -> out.append(literal.image());
            case ELNode.FloatingPointLiteral literal -> out.append(literal.image());
            case ELNode.StringLiteral literal -> appendString(out, literal.value());
            case ELNode.Identifier identifier -> out.append(identifier.name());
            case ELNode.Function function -> {
                if (!function.prefix().isEmpty()) {
                    out.append(function.prefix()).append(':');
                }
                out.append(function.localName());
                for (List<ELNode> invocation : function.invocations()) {
                    appendArguments(out, invocation);
                }
            }
            case ELNode.Property property -> {
                append(out, property.base());
                out.append('[');
                append(out, property.property());
                out.append(']');
            }
            case ELNode.Method method -> {
                append(out, method.base());
                out.append('[');
                append(out, method.property());
                out.append(']');
                appendArguments(out, method.arguments());
            }
            case ELNode.Call call -> {
                append(out, call.target());
                appendArguments(out, call.arguments());
            }
            case ELNode.Unary unary -> {
                out.append('(').append(symbol(unary.operator()));
                append(out, unary.operand());
                out.append(')');
            }
            case ELNode.Binary binary -> {
                out.append('(');
                append(out, binary.left());
                out.append(symbol(binary.operator()));
                append(out, binary.right());
                out.append(')');
            }
            case ELNode.Ternary ternary -> {
                out.append('(');
                append(out, ternary.condition());
                out.append('?');
                append(out, ternary.ifTrue());
                out.append(':');
                append(out, ternary.ifFalse());
                out.append(')');
            }
            case ELNode.Assign assign -> {
                out.append('(');
                append(out, assign.target());
                out.append('=');
                append(out, assign.value());
                out.append(')');
            }
            case ELNode.Semicolon semicolon -> {
                out.append('(');
                append(out, semicolon.left());
                out.append(';');
                append(out, semicolon.right());
                out.append(')');
            }
            case ELNode.Lambda lambda -> {
                out.append('(').append('(').append(String.join(",", lambda.parameters())).append(")->");
                append(out, lambda.body());
                out.append(')');
            }
            case ELNode.SetData set -> {
                out.append('{');
                appendList(out, set.elements());
                out.append('}');
            }
            case ELNode.ListData list -> {
                out.append('[');
                appendList(out, list.elements());
                out.append(']');
            }
            case ELNode.MapData map -> {
                out.append('{');
                boolean first = true;
                for (ELNode.MapData.MapEntry entry : map.entries()) {
                    if (!first) {
                        out.append(',');
                    }
                    first = false;
                    append(out, entry.key());
                    ELNode value = entry.value();
                    if (value != null) {
                        out.append(':');
                        append(out, value);
                    }
                }
                out.append('}');
            }
        }
    }

    private static void appendArguments(StringBuilder out, List<ELNode> arguments) {
        out.append('(');
        appendList(out, arguments);
        out.append(')');
    }

    private static void appendList(StringBuilder out, List<ELNode> nodes) {
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            append(out, nodes.get(i));
        }
    }

    private static void appendString(StringBuilder out, String value) {
        out.append('"');
        for (int i = 0; i < value.length(); i++) {
            char c = value.charAt(i);
            if (c == '"' || c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
        out.append('"');
    }

    private static void appendLiteralText(StringBuilder out, String text) {
        for (int i = 0; i < text.length(); i++) {
            char c = text.charAt(i);
            if ((c == '$' || c == '#') && i + 1 < text.length() && text.charAt(i + 1) == '{') {
                out.append('\\');
            } else if (c == '\\') {
                out.append('\\');
            }
            out.append(c);
        }
    }

    private static String symbol(UnaryOperator operator) {
        return switch (operator) {
            case NEGATE -> "-";
            case NOT -> "!";
            case EMPTY -> "empty ";
        };
    }

    private static String symbol(BinaryOperator operator) {
        return switch (operator) {
            case ADD -> "+";
            case SUBTRACT -> "-";
            case MULTIPLY -> "*";
            case DIVIDE -> "/";
            case MODULO -> "%";
            case CONCAT -> "+=";
            case LESS_THAN -> "<";
            case GREATER_THAN -> ">";
            case LESS_THAN_OR_EQUAL -> "<=";
            case GREATER_THAN_OR_EQUAL -> ">=";
            case EQUAL -> "==";
            case NOT_EQUAL -> "!=";
            case AND -> "&&";
            case OR -> "||";
        };
    }
}
