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
package io.micronaut.el.processor.compiler;

import io.micronaut.el.parser.ast.ELNode;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Stream;

/**
 * Structural analysis used before lowering an EL syntax tree.
 */
final class ELNodeAnalysis {

    private ELNodeAnalysis() {
    }

    static boolean containsLambda(ELNode node) {
        return node instanceof ELNode.Lambda || children(node).stream().anyMatch(ELNodeAnalysis::containsLambda);
    }

    static boolean hasSideEffects(ELNode node) {
        return switch (node) {
            case ELNode.Assign ignored -> true;
            case ELNode.Semicolon ignored -> true;
            default -> children(node).stream().anyMatch(ELNodeAnalysis::hasSideEffects);
        };
    }

    static void countIdentifiers(ELNode node, Map<String, Integer> direct, Map<String, Integer> lambdas) {
        countIdentifiers(node, direct, lambdas, false, Set.of());
    }

    private static void countIdentifiers(ELNode node,
                                         Map<String, Integer> direct,
                                         Map<String, Integer> lambdas,
                                         boolean inLambda,
                                         Set<String> parameters) {
        switch (node) {
            case ELNode.Identifier identifier -> {
                if (!parameters.contains(identifier.name())) {
                    (inLambda ? lambdas : direct).merge(identifier.name(), 1, Integer::sum);
                }
            }
            // a lambda outside one is counted where it is invoked, with its arguments bound
            case ELNode.Lambda lambda -> {
                if (inLambda) {
                    countIdentifiers(lambda.body(), direct, lambdas, true, bound(parameters, lambda));
                }
            }
            case ELNode.Method method -> countMethodIdentifiers(method, direct, lambdas, inLambda, parameters);
            default -> children(node).forEach(child -> countIdentifiers(child, direct, lambdas, inLambda, parameters));
        }
    }

    /**
     * Counts the identifiers of an invocation, whose lambda arguments are invoked by the method it calls and
     * are therefore counted as a lambda body rather than as part of the expression around them.
     */
    private static void countMethodIdentifiers(ELNode.Method method,
                                               Map<String, Integer> direct,
                                               Map<String, Integer> lambdas,
                                               boolean inLambda,
                                               Set<String> parameters) {
        countIdentifiers(method.base(), direct, lambdas, inLambda, parameters);
        countIdentifiers(method.property(), direct, lambdas, inLambda, parameters);
        for (ELNode argument : method.arguments()) {
            if (argument instanceof ELNode.Lambda lambda) {
                countIdentifiers(lambda.body(), direct, lambdas, true, bound(parameters, lambda));
            } else {
                countIdentifiers(argument, direct, lambdas, inLambda, parameters);
            }
        }
    }

    private static Set<String> bound(Set<String> parameters, ELNode.Lambda lambda) {
        Set<String> all = new HashSet<>(parameters);
        all.addAll(lambda.parameters());
        return all;
    }

    private static List<ELNode> children(ELNode node) {
        return switch (node) {
            case ELNode.Composite composite -> composite.parts();
            case ELNode.Eval eval -> List.of(eval.expression());
            case ELNode.Property property -> List.of(property.base(), property.property());
            case ELNode.Method method -> concat(List.of(method.base(), method.property()), method.arguments());
            case ELNode.Call call -> concat(List.of(call.target()), call.arguments());
            case ELNode.Function function -> function.invocations().stream().flatMap(List::stream).toList();
            case ELNode.Unary unary -> List.of(unary.operand());
            case ELNode.Binary binary -> List.of(binary.left(), binary.right());
            case ELNode.Ternary ternary -> List.of(ternary.condition(), ternary.ifTrue(), ternary.ifFalse());
            case ELNode.Assign assign -> List.of(assign.target(), assign.value());
            case ELNode.Semicolon semicolon -> List.of(semicolon.left(), semicolon.right());
            case ELNode.Lambda lambda -> List.of(lambda.body());
            case ELNode.SetData set -> set.elements();
            case ELNode.ListData list -> list.elements();
            case ELNode.MapData map -> map.entries().stream()
                .flatMap(entry -> entry.value() == null ? Stream.of(entry.key()) : Stream.of(entry.key(), entry.value()))
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
