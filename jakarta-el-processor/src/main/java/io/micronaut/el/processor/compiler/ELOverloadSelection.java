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
import io.micronaut.el.processor.visitor.ELTypes;
import io.micronaut.inject.ast.ClassElement;
import io.micronaut.inject.ast.ElementQuery;
import io.micronaut.inject.ast.MethodElement;
import io.micronaut.inject.ast.ParameterElement;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The overload selection of the section 1.2.1.2 of the specification, which follows the section 15.12.2 of the
 * JLS, over the static types of the arguments: the methods applicable by exact type, by subtyping, by coercion
 * and by variable arity, in that order, and the most specific of the first non-empty group.
 */
final class ELOverloadSelection {

    private static final MethodSelection NO_SELECTION = new MethodSelection(null, false);

    private final Function<ClassElement, @Nullable MethodElement> functionalMethod;

    /**
     * @param functionalMethod The single abstract method of a functional interface, or null for another type
     */
    ELOverloadSelection(Function<ClassElement, @Nullable MethodElement> functionalMethod) {
        this.functionalMethod = functionalMethod;
    }

    static List<MethodElement> methodCandidates(ClassElement type,
                                                String name,
                                                List<ELNode> arguments,
                                                boolean onlyStatic) {
        List<MethodElement> candidates = new ArrayList<>();
        for (MethodElement method : type.getEnclosedElements(ElementQuery.ALL_METHODS.onlyAccessible().named(name))) {
            if ((method.getParameters().length == arguments.size()
                || method.isVarArgs() && arguments.size() >= method.getParameters().length - 1) && method.isPublic()
                && (!onlyStatic || ELTypes.isStatic(method))) {
                candidates.add(method);
            }
        }
        return candidates;
    }

    /**
     * @param candidates    The methods of the name and arity
     * @param arguments     The arguments
     * @param argumentTypes The static types of the arguments, null for a lambda expression or an unknown type
     * @return The selection
     */
    MethodSelection select(List<MethodElement> candidates, List<ELNode> arguments, List<@Nullable ClassElement> argumentTypes) {
        if (candidates.isEmpty()) {
            return NO_SELECTION;
        }
        List<MethodElement> exact = new ArrayList<>();
        List<MethodElement> assignable = new ArrayList<>();
        List<MethodElement> coercible = new ArrayList<>();
        List<MethodElement> varArgs = new ArrayList<>();
        for (MethodElement candidate : candidates) {
            MethodMatch match = match(candidate, arguments, argumentTypes);
            switch (match) {
                case EXACT -> exact.add(candidate);
                case ASSIGNABLE -> assignable.add(candidate);
                case COERCIBLE -> coercible.add(candidate);
                case VARARGS -> varArgs.add(candidate);
                case NONE -> {
                    // not a candidate
                }
                default -> throw new IllegalStateException("Unexpected method match " + match);
            }
        }
        boolean elSpecific = exact.isEmpty() && assignable.isEmpty();
        List<MethodElement> best = !exact.isEmpty() ? exact : !assignable.isEmpty() ? assignable
            : !coercible.isEmpty() ? coercible : varArgs;
        return mostSpecific(best, argumentTypes, elSpecific);
    }

    // the overload selection of the section 1.6, whose branches are rules ordered against each other
    @SuppressWarnings("java:S3776")
    private MethodMatch match(MethodElement method,
                              List<ELNode> arguments,
                              List<@Nullable ClassElement> argumentTypes) {
        ParameterElement[] parameters = method.getParameters();
        int fixed = method.isVarArgs() ? parameters.length - 1 : parameters.length;
        if (method.isVarArgs() ? arguments.size() < fixed : arguments.size() != fixed) {
            return MethodMatch.NONE;
        }
        // the section 15.12.2 of the JLS, which the section 1.2.1.2 of the specification follows: an array assignable
        // to the variable arity parameter is the array itself, applicable in the first phase, by subtyping
        boolean directVarargsArray = method.isVarArgs() && arguments.size() == parameters.length
            && !(arguments.get(parameters.length - 1) instanceof ELNode.Lambda)
            && argumentTypes.get(parameters.length - 1) != null
            && ELTypeCompatibility.isArrayArgument(argumentTypes.get(parameters.length - 1), parameters[parameters.length - 1].getType());
        MethodMatch result = MethodMatch.EXACT;
        for (int i = 0; i < arguments.size(); i++) {
            ClassElement argument = argumentTypes.get(i);
            boolean directArray = directVarargsArray && i == fixed;
            ClassElement parameter = directArray ? parameters[parameters.length - 1].getType()
                : method.isVarArgs() && i >= fixed ? parameters[parameters.length - 1].getType().fromArray() : parameters[i].getType();
            MethodMatch argumentMatch;
            if (arguments.get(i) instanceof ELNode.Lambda) {
                // a lambda expression is compiled to a functional interface, section 1.25.8, and is itself a
                // LambdaExpression, the former being the direct form
                if (functionalMethod.apply(parameter) != null) {
                    argumentMatch = MethodMatch.EXACT;
                } else if (parameter.isAssignable(LambdaExpression.class) || parameter.getName().equals(Object.class.getName())) {
                    argumentMatch = MethodMatch.ASSIGNABLE;
                } else {
                    return MethodMatch.NONE;
                }
            } else if (arguments.get(i) instanceof ELNode.NullLiteral) {
                // a null is assignable to any parameter, a primitive one included, which the coercion of the
                // section 1.23 turns into its default: the interpreter selects int over Object for it, as the
                // reference implementations do, where Java would rule the primitive out
                argumentMatch = MethodMatch.ASSIGNABLE;
            } else if (directArray) {
                argumentMatch = ELTypeCompatibility.sameType(argument, parameter) ? MethodMatch.EXACT : MethodMatch.ASSIGNABLE;
            } else {
                if (argument == null) {
                    return MethodMatch.NONE;
                }
                argumentMatch = ELTypeCompatibility.sameType(argument, parameter) ? MethodMatch.EXACT
                    : ELSpecificity.sameBoxedType(argument, parameter) || ELTypeCompatibility.isAssignable(argument, parameter)
                        ? MethodMatch.ASSIGNABLE
                        : ELTypeCompatibility.isCoercible(argument, parameter) || ELTypeCompatibility.mayBeInstance(argument, parameter)
                            ? MethodMatch.COERCIBLE : MethodMatch.NONE;
            }
            if (argumentMatch == MethodMatch.NONE) {
                return MethodMatch.NONE;
            }
            if (argumentMatch == MethodMatch.COERCIBLE
                || (argumentMatch == MethodMatch.ASSIGNABLE && result == MethodMatch.EXACT)) {
                result = argumentMatch;
            }
        }
        return method.isVarArgs() && !directVarargsArray ? MethodMatch.VARARGS : result;
    }

    private static MethodSelection mostSpecific(List<MethodElement> candidates,
                                                List<@Nullable ClassElement> argumentTypes,
                                                boolean elSpecific) {
        if (candidates.isEmpty()) {
            return NO_SELECTION;
        }
        List<MethodElement> best = new ArrayList<>(1);
        for (MethodElement candidate : candidates) {
            boolean lessSpecific = false;
            for (int i = best.size() - 1; i >= 0; i--) {
                int comparison = ELSpecificity.compare(candidate, best.get(i), argumentTypes, elSpecific);
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

    private enum MethodMatch {
        EXACT,
        ASSIGNABLE,
        COERCIBLE,
        VARARGS,
        NONE
    }
}
