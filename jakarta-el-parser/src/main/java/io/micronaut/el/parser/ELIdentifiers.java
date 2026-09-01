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
import io.micronaut.el.parser.ast.ELNode;

import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;

/**
 * Finds identifiers whose variable-mapper bindings are fixed when an expression is created.
 */
@Internal
public final class ELIdentifiers {

    private ELIdentifiers() {
    }

    /**
     * Finds the free identifiers of a parsed expression.
     *
     * @param node The parsed expression
     * @return The free identifier names in encounter order
     */
    public static Set<String> free(ELNode node) {
        Set<String> identifiers = new LinkedHashSet<>();
        collect(node, Set.of(), identifiers);
        return Collections.unmodifiableSet(identifiers);
    }

    private static void collect(ELNode node, Set<String> lambdaParameters, Set<String> identifiers) {
        if (node instanceof ELNode.Identifier identifier) {
            if (!lambdaParameters.contains(identifier.name())) {
                identifiers.add(identifier.name());
            }
            return;
        }
        if (node instanceof ELNode.Function function && function.prefix().isEmpty()
            && !lambdaParameters.contains(function.localName())) {
            identifiers.add(function.localName());
        }
        if (node instanceof ELNode.Lambda lambda) {
            Set<String> nested = new LinkedHashSet<>(lambdaParameters);
            nested.addAll(lambda.parameters());
            collect(lambda.body(), nested, identifiers);
            return;
        }
        for (ELNode child : children(node)) {
            collect(child, lambdaParameters, identifiers);
        }
    }

    @SuppressWarnings("java:S1541")
    private static List<ELNode> children(ELNode node) {
        return switch (node) {
            case ELNode.Composite composite -> composite.parts();
            case ELNode.Eval eval -> List.of(eval.expression());
            case ELNode.Function function -> function.invocations().stream().flatMap(List::stream).toList();
            case ELNode.Property property -> List.of(property.base(), property.property());
            case ELNode.Method method -> concat(List.of(method.base(), method.property()), method.arguments());
            case ELNode.Call call -> concat(List.of(call.target()), call.arguments());
            case ELNode.Unary unary -> List.of(unary.operand());
            case ELNode.Binary binary -> List.of(binary.left(), binary.right());
            case ELNode.Ternary ternary -> List.of(ternary.condition(), ternary.ifTrue(), ternary.ifFalse());
            case ELNode.Assign assign -> List.of(assign.target(), assign.value());
            case ELNode.Semicolon semicolon -> List.of(semicolon.left(), semicolon.right());
            case ELNode.SetData setData -> setData.elements();
            case ELNode.ListData listData -> listData.elements();
            case ELNode.MapData mapData -> mapData.entries().stream()
                .flatMap(entry -> entry.value() == null
                    ? java.util.stream.Stream.of(entry.key())
                    : java.util.stream.Stream.of(entry.key(), entry.value()))
                .toList();
            default -> List.of();
        };
    }

    private static List<ELNode> concat(List<ELNode> first, List<ELNode> second) {
        List<ELNode> all = new ArrayList<>(first);
        all.addAll(second);
        return all;
    }
}
