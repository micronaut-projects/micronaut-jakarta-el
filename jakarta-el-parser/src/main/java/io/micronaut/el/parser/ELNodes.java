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
import java.util.function.BiFunction;
import org.jspecify.annotations.Nullable;

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
        return canonical(node, (prefix, localName) -> null);
    }

    /**
     * Renders the canonical form while replacing mapped function names with their bound-method identities.
     * The Jakarta EL equality contract compares bound functions rather than the prefixes used to name them.
     *
     * @param node          The parsed expression
     * @param functionName  Maps a function prefix and local name to its bound identity, or {@code null} to
     *                      retain the parsed name
     * @return The canonical form
     */
    public static String canonical(ELNode node, BiFunction<String, String, @Nullable String> functionName) {
        StringBuilder builder = new StringBuilder();
        append(builder, node, functionName);
        return builder.toString();
    }

    /**
     * Builds the stable identity of a method bound as a function.
     *
     * @param owner          The declaring class name
     * @param name           The method name
     * @param parameterTypes The parameter type names
     * @return The identity
     */
    public static String functionIdentity(String owner, String name, List<String> parameterTypes) {
        return owner + "#" + name + "(" + String.join(",", parameterTypes) + ")";
    }

    @SuppressWarnings("java:S1541")
    private static void append(StringBuilder out,
                               ELNode node,
                               BiFunction<String, String, @Nullable String> functionName) {
        switch (node) {
            case ELNode.Composite composite -> composite.parts().forEach(part -> append(out, part, functionName));
            case ELNode.LiteralText literal -> appendLiteralText(out, literal.text());
            case ELNode.Eval eval -> {
                out.append("${");
                append(out, eval.expression(), functionName);
                out.append('}');
            }
            case ELNode.NullLiteral ignored -> out.append("null");
            case ELNode.BooleanLiteral literal -> out.append(literal.value());
            case ELNode.IntegerLiteral literal -> out.append(literal.image());
            case ELNode.FloatingPointLiteral literal -> out.append(literal.image());
            case ELNode.StringLiteral literal -> appendString(out, literal.value());
            case ELNode.Identifier identifier -> out.append(identifier.name());
            case ELNode.Function function -> {
                String mapped = functionName.apply(function.prefix(), function.localName());
                if (mapped != null) {
                    out.append(mapped);
                } else {
                    if (!function.prefix().isEmpty()) {
                        out.append(function.prefix()).append(':');
                    }
                    out.append(function.localName());
                }
                for (List<ELNode> invocation : function.invocations()) {
                    appendArguments(out, invocation, functionName);
                }
            }
            case ELNode.Property property -> {
                append(out, property.base(), functionName);
                out.append('[');
                append(out, property.property(), functionName);
                out.append(']');
            }
            case ELNode.Method method -> {
                append(out, method.base(), functionName);
                out.append('[');
                append(out, method.property(), functionName);
                out.append(']');
                appendArguments(out, method.arguments(), functionName);
            }
            case ELNode.Call call -> {
                append(out, call.target(), functionName);
                appendArguments(out, call.arguments(), functionName);
            }
            case ELNode.Unary unary -> {
                out.append('(').append(symbol(unary.operator()));
                append(out, unary.operand(), functionName);
                out.append(')');
            }
            case ELNode.Binary binary -> {
                out.append('(');
                append(out, binary.left(), functionName);
                out.append(symbol(binary.operator()));
                append(out, binary.right(), functionName);
                out.append(')');
            }
            case ELNode.Ternary ternary -> {
                out.append('(');
                append(out, ternary.condition(), functionName);
                out.append('?');
                append(out, ternary.ifTrue(), functionName);
                out.append(':');
                append(out, ternary.ifFalse(), functionName);
                out.append(')');
            }
            case ELNode.Assign assign -> {
                out.append('(');
                append(out, assign.target(), functionName);
                out.append('=');
                append(out, assign.value(), functionName);
                out.append(')');
            }
            case ELNode.Semicolon semicolon -> {
                out.append('(');
                append(out, semicolon.left(), functionName);
                out.append(';');
                append(out, semicolon.right(), functionName);
                out.append(')');
            }
            case ELNode.Lambda lambda -> {
                out.append('(').append('(').append(String.join(",", lambda.parameters())).append(")->");
                append(out, lambda.body(), functionName);
                out.append(')');
            }
            case ELNode.SetData set -> {
                out.append('{');
                appendList(out, set.elements(), functionName);
                out.append('}');
            }
            case ELNode.ListData list -> {
                out.append('[');
                appendList(out, list.elements(), functionName);
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
                    append(out, entry.key(), functionName);
                    ELNode value = entry.value();
                    if (value != null) {
                        out.append(':');
                        append(out, value, functionName);
                    }
                }
                out.append('}');
            }
        }
    }

    private static void appendArguments(StringBuilder out,
                                        List<ELNode> arguments,
                                        BiFunction<String, String, @Nullable String> functionName) {
        out.append('(');
        appendList(out, arguments, functionName);
        out.append(')');
    }

    private static void appendList(StringBuilder out,
                                   List<ELNode> nodes,
                                   BiFunction<String, String, @Nullable String> functionName) {
        for (int i = 0; i < nodes.size(); i++) {
            if (i > 0) {
                out.append(',');
            }
            append(out, nodes.get(i), functionName);
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
