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

    // one case per node that can carry an identifier, which is the shape of the tree
    @SuppressWarnings("java:S3776")
    private static void countIdentifiers(ELNode node,
                                         Map<String, Integer> direct,
                                         Map<String, Integer> lambdas,
                                         boolean inLambda,
                                         Set<String> parameters) {
        if (node instanceof ELNode.Identifier identifier) {
            if (!parameters.contains(identifier.name())) {
                (inLambda ? lambdas : direct).merge(identifier.name(), 1, Integer::sum);
            }
        } else if (node instanceof ELNode.Lambda lambda) {
            if (inLambda) {
                countIdentifiers(lambda.body(), direct, lambdas, true, bound(parameters, lambda));
            }
        } else if (node instanceof ELNode.Method method) {
            countIdentifiers(method.base(), direct, lambdas, inLambda, parameters);
            countIdentifiers(method.property(), direct, lambdas, inLambda, parameters);
            for (ELNode argument : method.arguments()) {
                if (argument instanceof ELNode.Lambda lambda) {
                    countIdentifiers(lambda.body(), direct, lambdas, true, bound(parameters, lambda));
                } else {
                    countIdentifiers(argument, direct, lambdas, inLambda, parameters);
                }
            }
        } else {
            children(node).forEach(child -> countIdentifiers(child, direct, lambdas, inLambda, parameters));
        }
    }

    /**
     * The identifiers the evaluation of the expression resolves whatever its operands evaluate to: the operands
     * of ?:, && and || that may not be taken, the deferred right operand of {@code <} and {@code >}, and what
     * follows a base that may be null, its property and arguments, are left out, as are the lambda bodies.
     *
     * @param node The expression
     * @return The names of the identifiers always resolved
     */
    static Set<String> unconditionalIdentifiers(ELNode node) {
        Set<String> names = new HashSet<>();
        collectUnconditional(node, names);
        return names;
    }

    private static void collectUnconditional(ELNode node, Set<String> names) {
        switch (node) {
            case ELNode.Identifier identifier -> names.add(identifier.name());
            case ELNode.Lambda ignored -> {
                // evaluated when invoked, if at all
            }
            case ELNode.Property property -> collectUnconditional(property.base(), names);
            case ELNode.Method method -> collectUnconditional(method.base(), names);
            case ELNode.Ternary ternary -> collectUnconditional(ternary.condition(), names);
            case ELNode.Binary binary -> {
                collectUnconditional(binary.left(), names);
                switch (binary.operator()) {
                    case AND, OR, LESS_THAN, GREATER_THAN -> {
                        // short-circuited, or deferred when the left operand is null
                    }
                    default -> collectUnconditional(binary.right(), names);
                }
            }
            case ELNode.Semicolon semicolon -> collectUnconditional(semicolon.left(), names);
            default -> children(node).forEach(child -> collectUnconditional(child, names));
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
