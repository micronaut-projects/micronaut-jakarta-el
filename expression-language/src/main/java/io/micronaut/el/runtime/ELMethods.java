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
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

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

    private ELMethods() {
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
        if (paramTypes != null) {
            for (Method method : type.getMethods()) {
                if (method.getName().equals(name) && Arrays.equals(method.getParameterTypes(), paramTypes)) {
                    return method;
                }
            }
            throw notFound(type, name, paramTypes.length);
        }
        Object[] values = arguments == null ? new Object[0] : arguments;
        List<Method> best = new ArrayList<>();
        long bestScore = Long.MAX_VALUE;
        for (Method method : type.getMethods()) {
            if (!method.getName().equals(name)) {
                continue;
            }
            long score = score(method, values);
            if (score == NO_MATCH) {
                continue;
            }
            if (score < bestScore) {
                bestScore = score;
                best.clear();
                best.add(method);
            } else if (score == bestScore && !overrides(best, method)) {
                best.add(method);
            }
        }
        if (best.isEmpty()) {
            throw notFound(type, name, values.length);
        }
        if (best.size() > 1) {
            throw new MethodNotFoundException("The reference to the method '" + name + "' of " + type.getName()
                + " is ambiguous, " + best.size() + " methods match the arguments");
        }
        return best.get(0);
    }

    /**
     * Scores how well the arguments fit a method, the lower the better, {@link #NO_MATCH} when they do not fit.
     */
    private static long score(Method method, Object[] arguments) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        boolean varArgs = method.isVarArgs();
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
            Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
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
    private static boolean overrides(List<Method> methods, Method candidate) {
        for (Method method : methods) {
            if (Arrays.equals(method.getParameterTypes(), candidate.getParameterTypes())) {
                return true;
            }
        }
        return false;
    }

    private static MethodNotFoundException notFound(Class<?> type, String name, int count) {
        return new MethodNotFoundException("Cannot find the method '" + name + "' of " + type.getName()
            + " accepting " + count + " argument(s)");
    }
}
