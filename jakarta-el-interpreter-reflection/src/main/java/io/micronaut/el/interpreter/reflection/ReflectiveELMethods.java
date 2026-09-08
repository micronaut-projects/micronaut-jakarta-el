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
package io.micronaut.el.interpreter.reflection;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Finds and invokes a {@code java.lang.reflect.Method}, which is the selection of the section 1.6 of the
 * specification applied to what a class reports about itself.
 *
 * <p>This is the reflective half of {@code ELMethods}, which stayed in the core module with the parts that
 * need no reflection: the overload selection, the coercions and the metadata a registry of contributed methods
 * describes for itself. A runtime without this module resolves a method through the executors it was given
 * and never reads a class to find one.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ReflectiveELMethods {

    /**
     * The public methods of a class by name, each as its accessible declaration with its parameter types read
     * once: {@code Class.getMethods()} copies every method on every call, which is what makes the reflective
     * resolvers of the specification slow.
     */
    private static final ClassValue<Map<String, List<ELMethods.Candidate<Method>>>> METHODS = new ClassValue<>() {
        @Override
        protected Map<String, List<ELMethods.Candidate<Method>>> computeValue(Class<?> type) {
            Map<String, List<ELMethods.Candidate<Method>>> byName = new HashMap<>();
            for (Method method : type.getMethods()) {
                Method declaration = accessible(method);
                byName.computeIfAbsent(method.getName(), name -> new ArrayList<>(2))
                    .add(new ELMethods.Candidate<>(declaration, declaration.getParameterTypes(), declaration.isVarArgs(),
                        Modifier.isStatic(declaration.getModifiers()), declaration.isBridge()));
            }
            return byName;
        }
    };

    private ReflectiveELMethods() {
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
        List<ELMethods.Candidate<Method>> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            return null;
        }
        return ELMethods.select(type, name, candidates, paramTypes, arguments, isStatic);
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
        Object[] parameters = ELMethods.coerceArguments(context, method.getName(), method.getParameterTypes(),
            method.isVarArgs(), values);
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
        List<ELMethods.Candidate<Method>> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw ELMethods.notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return ELMethods.select(type, name, candidates, paramTypes, arguments, false);
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
        List<ELMethods.Candidate<Method>> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw ELMethods.notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return ELMethods.select(type, name, candidates, paramTypes, arguments, true);
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
}
