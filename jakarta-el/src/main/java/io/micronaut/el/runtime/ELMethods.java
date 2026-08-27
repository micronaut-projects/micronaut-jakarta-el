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
import jakarta.el.LambdaExpression;
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

    private static final int EXACT = 0;
    private static final int ASSIGNABLE = 1;
    private static final int COERCIBLE = 2;
    private static final int NO_MATCH = -1;

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
            if (count == parameterTypes.length && parameterTypes[fixed].isInstance(arguments[fixed])) {
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
        if (paramTypes != null) {
            for (Candidate candidate : candidates) {
                if ((!isStatic || candidate.isStatic()) && sameTypes(candidate.parameterTypes(), paramTypes)) {
                    return candidate.method();
                }
            }
            throw notFound(type, name, paramTypes.length);
        }
        Object[] values = arguments == null ? new Object[0] : arguments;
        List<Candidate> best = new ArrayList<>(1);
        long bestScore = Long.MAX_VALUE;
        for (Candidate candidate : candidates) {
            if (isStatic && !candidate.isStatic()) {
                continue;
            }
            long score = score(candidate, values);
            if (score == NO_MATCH) {
                continue;
            }
            if (score < bestScore) {
                bestScore = score;
                best.clear();
                best.add(candidate);
            } else if (score == bestScore && !overrides(best, candidate)) {
                best.add(candidate);
            }
        }
        if (best.isEmpty()) {
            throw notFound(type, name, values.length);
        }
        if (best.size() > 1) {
            throw new MethodNotFoundException("The reference to the method '" + name + "' of " + type.getName()
                + " is ambiguous, " + best.size() + " methods match the arguments");
        }
        return best.get(0).method();
    }

    /**
     * Compares declared parameter types with the ones provided at parse time, treating a primitive and its
     * wrapper as the same type: a class literal such as {@code double.class} cannot reach an annotation member
     * of a Micronaut annotation, so the wrapper is what a declaration can provide.
     */
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

    /**
     * Scores how well the arguments fit a method, the lower the better, {@link #NO_MATCH} when they do not fit.
     */
    private static long score(Candidate candidate, Object[] arguments) {
        Class<?>[] parameterTypes = candidate.parameterTypes();
        boolean varArgs = candidate.varArgs();
        if (varArgs ? arguments.length < parameterTypes.length - 1 : arguments.length != parameterTypes.length) {
            return NO_MATCH;
        }
        // a variable arity method is only considered once every fixed arity method has been ruled out
        long score = varArgs ? 1L << 32 : 0;
        int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
        for (int i = 0; i < fixed; i++) {
            int argument = score(parameterTypes[i], arguments[i]);
            if (argument == NO_MATCH) {
                return NO_MATCH;
            }
            score += argument;
        }
        if (varArgs) {
            Class<?> arrayType = parameterTypes[parameterTypes.length - 1];
            if (arguments.length == parameterTypes.length && arrayType.isInstance(arguments[fixed])) {
                return score + score(arrayType, arguments[fixed]);
            }
            Class<?> componentType = arrayType.getComponentType();
            for (int i = fixed; i < arguments.length; i++) {
                int argument = score(componentType, arguments[i]);
                if (argument == NO_MATCH) {
                    return NO_MATCH;
                }
                score += argument;
            }
        }
        return score;
    }

    private static int score(Class<?> parameterType, @Nullable Object argument) {
        if (argument == null) {
            return parameterType.isPrimitive() ? COERCIBLE : ASSIGNABLE;
        }
        Class<?> argumentType = argument.getClass();
        if (parameterType == argumentType) {
            return EXACT;
        }
        if (parameterType.isAssignableFrom(argumentType)) {
            return ASSIGNABLE;
        }
        if (argument instanceof LambdaExpression && parameterType.isInterface()) {
            // the section 1.25.8 of the specification coerces a lambda expression to a functional interface
            return COERCIBLE;
        }
        return isCoercible(parameterType) ? COERCIBLE : NO_MATCH;
    }

    private static boolean isCoercible(Class<?> parameterType) {
        return parameterType.isPrimitive()
            || Number.class.isAssignableFrom(parameterType)
            || parameterType == String.class
            || parameterType == Boolean.class
            || parameterType == Character.class
            || parameterType.isEnum();
    }

    /**
     * A method inherited from a supertype is the same method for the purpose of the reference.
     */
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
}
