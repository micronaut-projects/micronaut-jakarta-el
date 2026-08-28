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

import io.micronaut.core.annotation.Internal;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Picks the most specific of the overloads whose parameters already accept the arguments, as described in the
 * section 1.6 of the specification. {@link ELCompiler} decides which overloads are candidates; this decides
 * which of them the call is compiled against.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELMethodSpecificity {

    private ELMethodSpecificity() {
    }

    /**
     * Selects the uniquely most specific of the candidates.
     *
     * @param candidates    The overloads the arguments fit
     * @param argumentTypes The static types of the arguments, {@code null} where unknown
     * @param elSpecific    Whether a numeric parameter wins over a non-numeric one for a numeric argument
     * @return The selection, which reports no method both when there is none and when several are equal
     */
    static MethodSelection mostSpecific(List<MethodElement> candidates,
                                        List<ClassElement> argumentTypes,
                                        boolean elSpecific) {
        if (candidates.isEmpty()) {
            return new MethodSelection(null, false);
        }
        List<MethodElement> best = new ArrayList<>(1);
        for (MethodElement candidate : candidates) {
            boolean lessSpecific = false;
            for (int i = best.size() - 1; i >= 0; i--) {
                int comparison = compareSpecificity(candidate, best.get(i), argumentTypes, elSpecific);
                if (comparison > 0) {
                    best.remove(i);
                } else if (comparison < 0) {
                    lessSpecific = true;
                }
            }
            if (!lessSpecific && best.stream().noneMatch(method -> sameSignature(method, candidate))) {
                best.add(candidate);
            }
        }
        if (best.size() == 1) {
            return new MethodSelection(best.get(0), false);
        }
        return new MethodSelection(null, true);
    }

    /**
     * The parameter a method compares at a position: the declared one, or, past the fixed parameters of a
     * variable arity method, the component type of the array the call packs the arguments into.
     */
    static ClassElement comparisonType(MethodElement method, int index) {
        ParameterElement[] parameters = method.getParameters();
        if (method.isVarArgs() && index >= parameters.length - 1) {
            return parameters[parameters.length - 1].getType().fromArray();
        }
        return parameters[index].getType();
    }

    static boolean sameBoxedType(ClassElement first, ClassElement second) {
        return first.getName().equals(second.getName())
            || first.getName().equals(ELCompiler.wrapper(second.getName()))
            || ELCompiler.wrapper(first.getName()).equals(second.getName());
    }

    /**
     * @return The width of a numeric type, so that a wider parameter accepts a narrower argument, or -1
     */
    static int numericRank(ClassElement type) {
        return switch (type.getName()) {
            case "byte", "java.lang.Byte" -> 0;
            case "short", "java.lang.Short" -> 1;
            case "int", "java.lang.Integer" -> 2;
            case "long", "java.lang.Long" -> 3;
            case "float", "java.lang.Float" -> 4;
            case "double", "java.lang.Double" -> 5;
            default -> -1;
        };
    }

    /**
     * Which of two methods is the more specific one: the one whose every parameter is at least as specific as
     * the other's, or neither when they each win a position.
     */
    private static int compareSpecificity(MethodElement first,
                                          MethodElement second,
                                          List<ClassElement> argumentTypes,
                                          boolean elSpecific) {
        int length = Math.max(Math.max(first.getParameters().length, second.getParameters().length), argumentTypes.size());
        int result = 0;
        for (int i = 0; i < length; i++) {
            ClassElement firstType = comparisonType(first, i);
            ClassElement secondType = comparisonType(second, i);
            if (sameBoxedType(firstType, secondType)) {
                continue;
            }
            int comparison = compareParameter(firstType, secondType,
                i < argumentTypes.size() ? argumentTypes.get(i) : null, elSpecific);
            if (comparison == 0 || (result != 0 && result != comparison)) {
                return 0;
            }
            result = comparison;
        }
        return result;
    }

    /**
     * Which of two parameters of the same position is the more specific one: the one the other is assignable
     * to, or, when neither is, the numeric one for a numeric argument of an EL-specific call.
     */
    private static int compareParameter(ClassElement firstType,
                                        ClassElement secondType,
                                        @Nullable ClassElement argumentType,
                                        boolean elSpecific) {
        if (firstType.isAssignable(secondType)) {
            return 1;
        }
        if (secondType.isAssignable(firstType)) {
            return -1;
        }
        return numericSpecificity(firstType, secondType, argumentType, elSpecific);
    }

    private static int numericSpecificity(ClassElement first,
                                          ClassElement second,
                                          @Nullable ClassElement argument,
                                          boolean elSpecific) {
        if (!elSpecific || argument == null || numericRank(argument) < 0) {
            return 0;
        }
        boolean firstNumeric = numericRank(first) >= 0 || first.isAssignable(Number.class);
        boolean secondNumeric = numericRank(second) >= 0 || second.isAssignable(Number.class);
        return firstNumeric == secondNumeric ? 0 : firstNumeric ? 1 : -1;
    }

    private static boolean sameSignature(MethodElement first, MethodElement second) {
        ParameterElement[] firstParameters = first.getParameters();
        ParameterElement[] secondParameters = second.getParameters();
        if (firstParameters.length != secondParameters.length) {
            return false;
        }
        for (int i = 0; i < firstParameters.length; i++) {
            if (!firstParameters[i].getType().getName().equals(secondParameters[i].getType().getName())) {
                return false;
            }
        }
        return true;
    }

    /**
     * The result of selecting an overload, distinguishing no match from an ambiguous match.
     *
     * @param method    The uniquely selected method, or {@code null}
     * @param ambiguous Whether multiple methods matched with the same score
     */
    record MethodSelection(@Nullable MethodElement method, boolean ambiguous) {
    }
}
