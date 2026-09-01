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
package io.micronaut.el.resolver;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * The selection of an overload, and the coercion of the arguments to it, shared by the resolvers that read the
 * metadata Micronaut generates: the bean introspection of a type and the executable methods of a bean
 * definition describe their parameters the same way, as {@link Argument}s, so the two resolvers select an
 * overload and coerce the arguments identically.
 *
 * <p>Neither description carries the variable arity flag of the method, so an expanded variable arity call is
 * left to the reflective resolver later in the chain, which has that flag; otherwise a fixed array parameter
 * would incorrectly accept a scalar.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Internal
final class ELOverloads {

    private ELOverloads() {
    }

    /**
     * Selects the single overload of a name that accepts the arguments, following the order the section 1.6 of
     * the specification prefers: an overload whose parameters accept the arguments as they are, then the other
     * overloads of the same arity.
     *
     * @param context    The EL context
     * @param named      The overloads of the name
     * @param parameters The parameters of an overload
     * @param arguments  The evaluated arguments
     * @param <T>        The type describing a method
     * @return The selected overload, or {@code null} when none fits or the candidates are ambiguous, so that the
     * resolver declines and the rest of the chain gets its chance
     */
    @Nullable
    static <T> T select(ELContext context,
                        List<T> named,
                        Function<T, Argument<?>[]> parameters,
                        Object[] arguments) {
        // Coercing the arguments is part of selecting the overload, so it happens before the resolver commits:
        // an overload the arguments do not fit is skipped, and when none fits the resolver declines and the
        // standard resolvers get their chance.
        T selected = null;
        for (T candidate : named.size() == 1 ? named : candidates(named, parameters, arguments)) {
            if (coerce(context, parameters.apply(candidate), arguments) == null) {
                continue;
            }
            if (selected != null) {
                // The candidates have equal method-selection priority. Let the reflective resolver report
                // the ambiguity instead of depending on the order of the generated metadata.
                return null;
            }
            selected = candidate;
        }
        return selected;
    }

    /**
     * Selects the overload declaring exactly the parameter types the method expression was created with.
     *
     * @param context       The EL context
     * @param named         The overloads of the name
     * @param parameters    The parameters of an overload
     * @param argumentTypes The parameter types the expression was created with
     * @param arguments     The evaluated arguments
     * @param <T>           The type describing a method
     * @return The selected overload, or {@code null} when none declares those types or takes those arguments
     */
    @Nullable
    static <T> T declaring(ELContext context,
                           List<T> named,
                           Function<T, Argument<?>[]> parameters,
                           Argument<?>[] argumentTypes,
                           Object[] arguments) {
        for (T candidate : named) {
            Argument<?>[] declared = parameters.apply(candidate);
            if (sameTypes(declared, argumentTypes) && coerce(context, declared, arguments) != null) {
                return candidate;
            }
        }
        return null;
    }

    /**
     * Coerces arguments of the same arity to the declared parameters.
     *
     * @param context    The EL context
     * @param parameters The declared parameters
     * @param arguments  The evaluated arguments
     * @return The coerced arguments, or {@code null} when the arguments do not fit the parameters, so that the
     * overload is not selected
     */
    static Object @Nullable [] coerce(ELContext context, Argument<?>[] parameters, Object[] arguments) {
        if (arguments.length != parameters.length) {
            return null;
        }
        try {
            Object[] coerced = new Object[parameters.length];
            for (int i = 0; i < parameters.length; i++) {
                coerced[i] = ELSupport.coerceToType(context, arguments[i], parameters[i].getType());
            }
            return coerced;
        } catch (ELException e) {
            // this overload does not accept these arguments
            return null;
        }
    }

    /**
     * The overloads of the given name that can take the arguments, in the order the section 1.6 of the
     * specification prefers them.
     */
    private static <T> List<T> candidates(List<T> named,
                                          Function<T, Argument<?>[]> parameters,
                                          Object[] arguments) {
        List<T> exact = new ArrayList<>(2);
        List<T> fixedArity = new ArrayList<>(2);
        for (T candidate : named) {
            Argument<?>[] declared = parameters.apply(candidate);
            if (declared.length == arguments.length) {
                (accepts(declared, arguments) ? exact : fixedArity).add(candidate);
            }
        }
        if (!exact.isEmpty()) {
            return mostSpecific(exact, parameters);
        }
        return fixedArity;
    }

    /**
     * Returns the uniquely most specific method from the candidates that already accept every argument, or no
     * method when the candidates are ambiguous. Returning no method lets the reflective resolver report the
     * ambiguity according to the EL method-selection rules.
     */
    private static <T> List<T> mostSpecific(List<T> candidates, Function<T, Argument<?>[]> parameters) {
        T result = null;
        for (int candidateIndex = 0; candidateIndex < candidates.size(); candidateIndex++) {
            T candidate = candidates.get(candidateIndex);
            boolean mostSpecific = true;
            for (int otherIndex = 0; otherIndex < candidates.size(); otherIndex++) {
                if (candidateIndex != otherIndex
                    && !moreSpecific(parameters.apply(candidate), parameters.apply(candidates.get(otherIndex)))) {
                    mostSpecific = false;
                    break;
                }
            }
            if (!mostSpecific) {
                continue;
            }
            if (result != null) {
                return List.of();
            }
            result = candidate;
        }
        return result == null ? List.of() : List.of(result);
    }

    private static boolean moreSpecific(Argument<?>[] first, Argument<?>[] second) {
        for (int i = 0; i < first.length; i++) {
            if (!second[i].getWrapperType().isAssignableFrom(first[i].getWrapperType())) {
                return false;
            }
        }
        return true;
    }

    private static boolean accepts(Argument<?>[] parameters, Object[] arguments) {
        for (int i = 0; i < parameters.length; i++) {
            Object argument = arguments[i];
            Class<?> type = parameters[i].getWrapperType();
            if (argument == null ? parameters[i].getType().isPrimitive() : !type.isInstance(argument)) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameTypes(Argument<?>[] parameters, Argument<?>[] argumentTypes) {
        if (parameters.length != argumentTypes.length) {
            return false;
        }
        for (int i = 0; i < parameters.length; i++) {
            if (parameters[i].getWrapperType() != argumentTypes[i].getWrapperType()) {
                return false;
            }
        }
        return true;
    }
}
