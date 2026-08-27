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
package io.micronaut.el.runtime;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.reflect.ReflectionUtils;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Selects the method a method expression refers to, as described in the section 1.6 of the Jakarta
 * Expression Language specification.
 *
 * <p>When the parameter types are provided at parse time they select the method on their own. Otherwise the
 * evaluated arguments are used: an exact match is preferred over an assignable one, an assignable one over a
 * coercible one, and a method with a fixed arity over a variable arity method. A tie makes the reference
 * ambiguous, which is an error.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELMethods {

    /**
     * The public methods of a class by name, each as its accessible declaration with its parameter types read
     * once: {@code Class.getMethods()} copies every method on every call, which is what makes the reflective
     * resolvers of the specification slow.
     */
    private static final ClassValue<Map<String, List<Candidate>>> METHODS = new ClassValue<>() {
        @Override
        protected Map<String, List<Candidate>> computeValue(Class<?> type) {
            Map<String, List<Candidate>> byName = new HashMap<>();
            for (Method method : type.getMethods()) {
                Method declaration = accessible(method);
                byName.computeIfAbsent(method.getName(), name -> new ArrayList<>(2))
                    .add(new Candidate(declaration, declaration.getParameterTypes(), declaration.isVarArgs(),
                        Modifier.isStatic(declaration.getModifiers())));
            }
            return byName;
        }
    };

    private ELMethods() {
    }

    /**
     * Finds a method by name and arguments, as {@link #findMethod(Class, String, Class[], Object[])} does, or
     * returns {@code null} when the type declares no public method of the name.
     *
     * @param type       The type of the base object, or the class for a static method
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @param isStatic   Whether to look for a static method
     * @return The method, or {@code null} when the type declares no method of the name
     */
    @Nullable
    public static Method findMethodOrNull(Class<?> type,
                                          String name,
                                          Class<?> @Nullable [] paramTypes,
                                          Object @Nullable [] arguments,
                                          boolean isStatic) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            return null;
        }
        return select(type, name, candidates, paramTypes, arguments, isStatic);
    }

    /**
     * Invokes a method reflectively, the arguments coerced to the parameter types as described in the section
     * 1.23 of the specification, the variable arity arguments packed into an array, the exceptions of the
     * method unwrapped.
     *
     * @param context The context
     * @param method  The method
     * @param base    The base object, or {@code null} for a static method
     * @param values  The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invoke(ELContext context, Method method, @Nullable Object base, Object @Nullable [] values) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        Object[] arguments = values == null ? new Object[0] : values;
        int count = arguments.length;
        int fixed = method.isVarArgs() ? parameterTypes.length - 1 : parameterTypes.length;
        if (method.isVarArgs() ? count < fixed : count != fixed) {
            throw new IllegalArgumentException("The method '" + method.getName() + "' expects "
                + (method.isVarArgs() ? "at least " + fixed : fixed) + " argument(s) but " + count + " were provided");
        }
        for (int i = 0; i < fixed && i < count; i++) {
            parameters[i] = ELSupport.coerceToType(context, arguments[i], parameterTypes[i]);
        }
        if (method.isVarArgs()) {
            Class<?> componentType = parameterTypes[fixed].getComponentType();
            if (count == parameterTypes.length && arguments[fixed] != null
                && arguments[fixed].getClass() == parameterTypes[fixed]) {
                parameters[fixed] = arguments[fixed];
            } else {
                Object varargs = Array.newInstance(componentType, Math.max(0, count - fixed));
                for (int i = fixed; i < count; i++) {
                    Array.set(varargs, i - fixed, ELSupport.coerceToType(context, arguments[i], componentType));
                }
                parameters[fixed] = varargs;
            }
        }
        try {
            return method.invoke(base, parameters);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new ELException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause instanceof ELException elException ? elException : new ELException(cause);
        }
    }

    /**
     * Finds the method a method expression refers to.
     *
     * @param type       The type of the base object
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @return The method
     */
    public static Method findMethod(Class<?> type,
                                    String name,
                                    Class<?> @Nullable [] paramTypes,
                                    Object @Nullable [] arguments) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return select(type, name, candidates, paramTypes, arguments, false);
    }

    /**
     * Finds the static method a method expression refers to.
     *
     * @param type       The type declaring the method
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @return The method
     */
    public static Method findStaticMethod(Class<?> type,
                                          String name,
                                          Class<?> @Nullable [] paramTypes,
                                          Object @Nullable [] arguments) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return select(type, name, candidates, paramTypes, arguments, true);
    }

    private static Method select(Class<?> type,
                                 String name,
                                 List<Candidate> candidates,
                                 Class<?> @Nullable [] paramTypes,
                                 Object @Nullable [] arguments,
                                 boolean isStatic) {
        Object[] values = arguments == null ? new Object[0] : arguments;
        Class<?>[] matchingTypes = paramTypes == null ? Arrays.stream(values)
            .map(value -> value == null ? null : value.getClass())
            .toArray(Class<?>[]::new) : paramTypes;
        List<Candidate> assignable = new ArrayList<>();
        List<Candidate> coercible = new ArrayList<>();
        List<Candidate> varArgs = new ArrayList<>();
        for (Candidate candidate : candidates) {
            if (isStatic && !candidate.isStatic()) {
                continue;
            }
            Match match = match(candidate, matchingTypes, values);
            switch (match) {
                case EXACT -> {
                    return candidate.method();
                }
                case ASSIGNABLE -> assignable.add(candidate);
                case COERCIBLE -> coercible.add(candidate);
                case VARARGS -> varArgs.add(candidate);
                case NONE -> {
                    // not a candidate
                }
                default -> throw new IllegalStateException("Unexpected method match " + match);
            }
        }
        boolean elSpecific = assignable.isEmpty();
        List<Candidate> best = !assignable.isEmpty() ? assignable
            : !coercible.isEmpty() ? coercible : varArgs;
        if (best.isEmpty()) {
            throw notFound(type, name, matchingTypes.length);
        }
        return mostSpecific(type, name, best, matchingTypes, elSpecific).method();
    }

    /**
     * Compares parameter types, treating a primitive and its wrapper as the same type.
     *
     * @param declared The declared parameter types
     * @param provided The parameter types to match
     * @return Whether every declared type matches its provided counterpart
     */
    public static boolean sameTypes(Class<?>[] declared, Class<?>[] provided) {
        if (declared.length != provided.length) {
            return false;
        }
        for (int i = 0; i < declared.length; i++) {
            if (declared[i] != provided[i] && ReflectionUtils.getWrapperType(declared[i]) != ReflectionUtils.getWrapperType(provided[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a declaration of the method that can be invoked reflectively.
     *
     * <p>{@code Class.getMethods()} returns the method as declared on the runtime class, which may not be
     * accessible: the lists of {@code List.of}, the views of {@code Collections} and lambdas all implement public
     * interfaces from classes that are not public. The public declaration is found on a supertype, which is
     * what {@code jakarta.el.ELUtil} does for the standard resolvers.</p>
     *
     * @param method The method as declared on the runtime class
     * @return An accessible declaration of the same method
     */
    public static Method accessible(Method method) {
        if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return method;
        }
        Method found = findAccessible(method.getDeclaringClass(), method);
        return found == null ? method : found;
    }

    @Nullable
    private static Method findAccessible(Class<?> type, Method method) {
        for (Class<?> anInterface : type.getInterfaces()) {
            if (Modifier.isPublic(anInterface.getModifiers())) {
                try {
                    return anInterface.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ignored) {
                    // declared further up
                }
            }
            Method found = findAccessible(anInterface, method);
            if (found != null) {
                return found;
            }
        }
        Class<?> superclass = type.getSuperclass();
        if (superclass == null) {
            return null;
        }
        if (Modifier.isPublic(superclass.getModifiers())) {
            try {
                return superclass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
                // declared further up
            }
        }
        return findAccessible(superclass, method);
    }

    private static Match match(Candidate candidate, Class<?>[] matchingTypes, Object[] arguments) {
        Class<?>[] parameterTypes = candidate.parameterTypes();
        boolean varArgs = candidate.varArgs();
        if (varArgs ? matchingTypes.length < parameterTypes.length - 1 : matchingTypes.length != parameterTypes.length) {
            return Match.NONE;
        }
        Match match = Match.EXACT;
        int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
        for (int i = 0; i < fixed; i++) {
            Match argument = match(parameterTypes[i], matchingTypes[i], argument(arguments, i));
            if (argument == Match.NONE) {
                return Match.NONE;
            }
            if (argument == Match.COERCIBLE || (argument == Match.ASSIGNABLE && match == Match.EXACT)) {
                match = argument;
            }
        }
        if (varArgs) {
            Class<?> arrayType = parameterTypes[parameterTypes.length - 1];
            if (matchingTypes.length == parameterTypes.length && arrayType == matchingTypes[fixed]) {
                return Match.VARARGS;
            }
            Class<?> componentType = arrayType.getComponentType();
            for (int i = fixed; i < matchingTypes.length; i++) {
                if (match(componentType, matchingTypes[i], argument(arguments, i)) == Match.NONE) {
                    return Match.NONE;
                }
            }
            return Match.VARARGS;
        }
        return match;
    }

    private static Match match(Class<?> parameterType,
                               @Nullable Class<?> matchingType,
                               @Nullable Object argument) {
        if (parameterType == matchingType) {
            return Match.EXACT;
        }
        if (matchingType == null) {
            return parameterType.isPrimitive() ? Match.COERCIBLE : Match.ASSIGNABLE;
        }
        if (ReflectionUtils.getWrapperType(parameterType).isAssignableFrom(matchingType)) {
            return Match.ASSIGNABLE;
        }
        if (argument == null) {
            return Match.NONE;
        }
        try {
            ELSupport.coerce(argument, parameterType);
            return Match.COERCIBLE;
        } catch (ELException | IllegalArgumentException e) {
            return Match.NONE;
        }
    }

    @Nullable
    private static Object argument(Object[] arguments, int index) {
        return index < arguments.length ? arguments[index] : null;
    }

    private static Candidate mostSpecific(Class<?> type,
                                          String name,
                                          List<Candidate> candidates,
                                          Class<?>[] argumentTypes,
                                          boolean elSpecific) {
        List<Candidate> best = new ArrayList<>(1);
        for (Candidate candidate : candidates) {
            boolean lessSpecific = false;
            for (int i = best.size() - 1; i >= 0; i--) {
                int comparison = compareSpecificity(candidate, best.get(i), argumentTypes, elSpecific);
                if (comparison > 0) {
                    best.remove(i);
                } else if (comparison < 0) {
                    lessSpecific = true;
                }
            }
            if (!lessSpecific && !overrides(best, candidate)) {
                best.add(candidate);
            }
        }
        if (best.size() != 1) {
            throw new MethodNotFoundException("The reference to the method '" + name + "' of " + type.getName()
                + " is ambiguous, " + best.size() + " methods match the arguments");
        }
        return best.get(0);
    }

    private static int compareSpecificity(Candidate first,
                                          Candidate second,
                                          Class<?>[] argumentTypes,
                                          boolean elSpecific) {
        int length = Math.max(Math.max(first.parameterTypes().length, second.parameterTypes().length), argumentTypes.length);
        Class<?>[] firstTypes = comparisonTypes(first, length);
        Class<?>[] secondTypes = comparisonTypes(second, length);
        int result = 0;
        for (int i = 0; i < length; i++) {
            Class<?> firstType = ReflectionUtils.getWrapperType(firstTypes[i]);
            Class<?> secondType = ReflectionUtils.getWrapperType(secondTypes[i]);
            if (firstType == secondType) {
                continue;
            }
            int comparison = secondType.isAssignableFrom(firstType) ? 1
                : firstType.isAssignableFrom(secondType) ? -1
                : numericSpecificity(firstType, secondType,
                    i < argumentTypes.length ? argumentTypes[i] : null, elSpecific);
            if (comparison == 0 || (result != 0 && result != comparison)) {
                return 0;
            }
            result = comparison;
        }
        if (result == 0 && first.method().isBridge() != second.method().isBridge()) {
            return first.method().isBridge() ? -1 : 1;
        }
        return result;
    }

    private static Class<?>[] comparisonTypes(Candidate candidate, int length) {
        Class<?>[] declared = candidate.parameterTypes();
        if (!candidate.varArgs()) {
            return declared;
        }
        Class<?>[] expanded = Arrays.copyOf(declared, length);
        Class<?> component = declared[declared.length - 1].getComponentType();
        Arrays.fill(expanded, declared.length - 1, length, component);
        return expanded;
    }

    private static int numericSpecificity(Class<?> first,
                                          Class<?> second,
                                          @Nullable Class<?> argument,
                                          boolean elSpecific) {
        if (!elSpecific || argument == null || !Number.class.isAssignableFrom(argument)) {
            return 0;
        }
        boolean firstNumeric = Number.class.isAssignableFrom(first);
        boolean secondNumeric = Number.class.isAssignableFrom(second);
        return firstNumeric == secondNumeric ? 0 : firstNumeric ? 1 : -1;
    }

    private static boolean overrides(List<Candidate> candidates, Candidate candidate) {
        for (Candidate other : candidates) {
            if (Arrays.equals(other.parameterTypes(), candidate.parameterTypes())) {
                return true;
            }
        }
        return false;
    }

    private static MethodNotFoundException notFound(Class<?> type, String name, int count) {
        return new MethodNotFoundException("Cannot find the method '" + name + "' of " + type.getName()
            + " accepting " + count + " argument(s)");
    }

    /**
     * A public method of a class, with what the selection needs read once.
     *
     * @param method         The accessible declaration of the method
     * @param parameterTypes Its parameter types
     * @param varArgs        Whether it is of variable arity
     * @param isStatic       Whether it is static
     */
    private record Candidate(Method method, Class<?>[] parameterTypes, boolean varArgs, boolean isStatic) {
    }

    private enum Match {
        EXACT,
        ASSIGNABLE,
        COERCIBLE,
        VARARGS,
        NONE
    }
}
