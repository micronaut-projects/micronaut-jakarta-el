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
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELSupport;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.FunctionMapper;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.Arrays;

/**
 * Resolves interpreter methods through the public Java reflection API.
 *
 * <p>This executor is registered as a service and is intentionally kept in a separate artifact. Applications
 * that need arbitrary Java method execution can add this module; applications that provide generated or
 * otherwise direct executors can omit it.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ReflectiveELMethodExecutor implements ELMethodExecutor {

    @Override
    public int getPriority() {
        return -100;
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context,
                            @Nullable Object base,
                            @Nullable Object method,
                            Class<?> @Nullable [] paramTypes,
                            Object @Nullable [] arguments) {
        if (base == null || method == null) {
            return null;
        }
        String name = method.toString();
        if (base instanceof ELClass elClass) {
            if ("<init>".equals(name)) {
                Constructor<?> constructor = findConstructor(elClass.getKlass(), paramTypes, arguments);
                return constructor == null ? null : new ConstructorMethod(constructor);
            }
            Method resolved = ELMethods.findMethodOrNull(elClass.getKlass(), name, paramTypes, arguments, true);
            return resolved == null ? null : new ReflectiveMethod(ELMethods.accessible(resolved), true, false);
        }
        Method resolved = ELMethods.findMethodOrNull(base.getClass(), name, paramTypes, arguments, false);
        return resolved == null ? null : new ReflectiveMethod(ELMethods.accessible(resolved), false, false);
    }

    @Override
    @Nullable
    public ELMethod resolveFunction(ELContext context, String prefix, String localName) {
        FunctionMapper functionMapper = context.getFunctionMapper();
        if (functionMapper == null) {
            return null;
        }
        Method method = functionMapper.resolveFunction(prefix, localName);
        if (method == null) {
            return null;
        }
        return new ReflectiveMethod(ELMethods.accessible(method), Modifier.isStatic(method.getModifiers()), true);
    }

    @Nullable
    private static Constructor<?> findConstructor(Class<?> type,
                                                  Class<?> @Nullable [] paramTypes,
                                                  Object @Nullable [] arguments) {
        Constructor<?>[] constructors = type.getConstructors();
        if (paramTypes != null) {
            for (Constructor<?> constructor : constructors) {
                if (sameTypes(constructor.getParameterTypes(), paramTypes)) {
                    return constructor;
                }
            }
            return null;
        }
        Object[] values = arguments == null ? new Object[0] : arguments;
        Constructor<?> selected = null;
        int selectedScore = Integer.MAX_VALUE;
        for (Constructor<?> constructor : constructors) {
            int score = score(constructor.getParameterTypes(), constructor.isVarArgs(), values);
            if (score == Integer.MAX_VALUE) {
                continue;
            }
            if (score < selectedScore) {
                selected = constructor;
                selectedScore = score;
            } else if (score == selectedScore) {
                return null;
            }
        }
        return selected;
    }

    private static int score(Class<?>[] parameterTypes, boolean varArgs, Object[] arguments) {
        int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
        if (varArgs ? arguments.length < fixed : arguments.length != fixed) {
            return Integer.MAX_VALUE;
        }
        int score = varArgs ? 3 : 0;
        for (int i = 0; i < fixed; i++) {
            int argumentScore = score(parameterTypes[i], arguments[i]);
            if (argumentScore == Integer.MAX_VALUE) {
                return argumentScore;
            }
            score += argumentScore;
        }
        if (varArgs) {
            Class<?> componentType = parameterTypes[fixed].getComponentType();
            if (arguments.length == parameterTypes.length && arguments[fixed] != null
                && parameterTypes[fixed].isInstance(arguments[fixed])) {
                return score;
            }
            for (int i = fixed; i < arguments.length; i++) {
                int argumentScore = score(componentType, arguments[i]);
                if (argumentScore == Integer.MAX_VALUE) {
                    return argumentScore;
                }
                score += argumentScore;
            }
        }
        return score;
    }

    private static int score(Class<?> parameterType, @Nullable Object argument) {
        if (argument == null) {
            return parameterType.isPrimitive() ? 2 : 1;
        }
        Class<?> argumentType = argument.getClass();
        if (parameterType == argumentType) {
            return 0;
        }
        if (wrap(parameterType).isAssignableFrom(argumentType)) {
            return 1;
        }
        try {
            io.micronaut.el.runtime.ELSupport.coerce(argument, parameterType);
            return 2;
        } catch (ELException | IllegalArgumentException e) {
            return Integer.MAX_VALUE;
        }
    }

    private static boolean sameTypes(Class<?>[] declared, Class<?>[] provided) {
        if (declared.length != provided.length) {
            return false;
        }
        for (int i = 0; i < declared.length; i++) {
            if (wrap(declared[i]) != wrap(provided[i])) {
                return false;
            }
        }
        return true;
    }

    private static Class<?> wrap(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        return switch (type.getName()) {
            case "boolean" -> Boolean.class;
            case "byte" -> Byte.class;
            case "short" -> Short.class;
            case "int" -> Integer.class;
            case "long" -> Long.class;
            case "char" -> Character.class;
            case "float" -> Float.class;
            case "double" -> Double.class;
            case "void" -> Void.class;
            default -> type;
        };
    }

    private static final class ReflectiveMethod implements ELMethod {
        private final Class<?> owner;
        private final String name;
        private final Class<?>[] parameterTypes;
        private final boolean staticMethod;
        private final boolean directArrayVarargs;
        private transient @Nullable Method method;

        private ReflectiveMethod(Method method, boolean staticMethod, boolean directArrayVarargs) {
            Method accessible = accessible(method);
            this.owner = accessible.getDeclaringClass();
            this.name = accessible.getName();
            this.parameterTypes = accessible.getParameterTypes().clone();
            this.staticMethod = staticMethod;
            this.directArrayVarargs = directArrayVarargs;
            this.method = accessible;
        }

        @Override
        public String getName() {
            return name;
        }

        @Override
        public Class<?> getReturnType() {
            return method().getReturnType();
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return parameterTypes.clone();
        }

        @Override
        public boolean isVarArgs() {
            return method().isVarArgs();
        }

        @Override
        public java.lang.annotation.Annotation[] getAnnotations() {
            return method().getAnnotations();
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Method resolved = method();
            return directArrayVarargs
                ? invokeFunction(context, resolved, staticMethod ? null : base, arguments)
                : ELMethods.invoke(context, resolved, staticMethod ? null : base, arguments);
        }

        @Override
        public String identity() {
            return io.micronaut.el.parser.ELNodes.functionIdentity(owner.getName(), name,
                Arrays.stream(parameterTypes).map(Class::getTypeName).toList());
        }

        private Method method() {
            Method resolved = method;
            if (resolved == null) {
                resolved = ELMethods.findMethodOrNull(owner, name, parameterTypes, null, staticMethod);
                if (resolved == null) {
                    throw new ELException("Cannot restore the method '" + owner.getName() + '.' + name + "'");
                }
                method = accessible(resolved);
            }
            return resolved;
        }

        private static Method accessible(Method method) {
            Method accessible = ELMethods.accessible(method);
            if (!Modifier.isPublic(accessible.getDeclaringClass().getModifiers())) {
                accessible.trySetAccessible();
            }
            return accessible;
        }

        @Nullable
        private static Object invokeFunction(ELContext context,
                                              Method method,
                                              @Nullable Object base,
                                              Object @Nullable [] values) {
            if (!method.isVarArgs()) {
                return ELMethods.invoke(context, method, base, values);
            }
            Class<?>[] parameterTypes = method.getParameterTypes();
            Object[] arguments = values == null ? new Object[0] : values;
            int fixed = parameterTypes.length - 1;
            if (arguments.length < fixed) {
                throw new IllegalArgumentException("The method '" + method.getName() + "' expects at least "
                    + fixed + " argument(s) but " + arguments.length + " were provided");
            }
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < fixed; i++) {
                parameters[i] = ELSupport.coerceToType(context, arguments[i], parameterTypes[i]);
            }
            if (arguments.length == parameterTypes.length && arguments[fixed] != null
                && parameterTypes[fixed].isInstance(arguments[fixed])) {
                parameters[fixed] = arguments[fixed];
            } else {
                Class<?> componentType = parameterTypes[fixed].getComponentType();
                Object varargs = Array.newInstance(componentType, arguments.length - fixed);
                for (int i = fixed; i < arguments.length; i++) {
                    Array.set(varargs, i - fixed, ELSupport.coerceToType(context, arguments[i], componentType));
                }
                parameters[fixed] = varargs;
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
    }

    private static final class ConstructorMethod implements ELMethod {
        private final Class<?> owner;
        private final Class<?>[] parameterTypes;
        private final boolean varArgs;
        private transient @Nullable Constructor<?> constructor;

        private ConstructorMethod(Constructor<?> constructor) {
            this.owner = constructor.getDeclaringClass();
            this.parameterTypes = constructor.getParameterTypes().clone();
            this.varArgs = constructor.isVarArgs();
            this.constructor = constructor;
        }

        @Override
        public String getName() {
            return "<init>";
        }

        @Override
        public Class<?> getReturnType() {
            return owner;
        }

        @Override
        public Class<?>[] getParameterTypes() {
            return parameterTypes.clone();
        }

        @Override
        public boolean isVarArgs() {
            return varArgs;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Object[] values = arguments == null ? new Object[0] : arguments;
            int fixed = varArgs ? parameterTypes.length - 1 : parameterTypes.length;
            if (varArgs ? values.length < fixed : values.length != fixed) {
                throw new IllegalArgumentException("The constructor of '" + owner.getName() + "' expects "
                    + (varArgs ? "at least " + fixed : fixed) + " argument(s) but " + values.length + " were provided");
            }
            Object[] parameters = new Object[parameterTypes.length];
            for (int i = 0; i < fixed; i++) {
                parameters[i] = io.micronaut.el.runtime.ELSupport.coerceToType(context, values[i], parameterTypes[i]);
            }
            if (varArgs) {
                if (values.length == parameterTypes.length && values[fixed] != null
                    && parameterTypes[fixed].isInstance(values[fixed])) {
                    parameters[fixed] = values[fixed];
                } else {
                    Class<?> componentType = parameterTypes[fixed].getComponentType();
                    Object varargs = java.lang.reflect.Array.newInstance(componentType, values.length - fixed);
                    for (int i = fixed; i < values.length; i++) {
                        io.micronaut.el.runtime.ELArray.set(varargs, i - fixed,
                            io.micronaut.el.runtime.ELSupport.coerceToType(context, values[i], componentType));
                    }
                    parameters[fixed] = varargs;
                }
            }
            try {
                return constructor().newInstance(parameters);
            } catch (InstantiationException | IllegalAccessException | IllegalArgumentException e) {
                throw new ELException(e);
            } catch (InvocationTargetException e) {
                Throwable cause = e.getCause();
                throw cause instanceof ELException elException ? elException : new ELException(cause);
            }
        }

        private Constructor<?> constructor() {
            Constructor<?> resolved = constructor;
            if (resolved == null) {
                try {
                    resolved = owner.getConstructor(parameterTypes);
                } catch (NoSuchMethodException e) {
                    throw new ELException("Cannot restore a constructor of '" + owner.getName() + "'", e);
                }
                constructor = resolved;
            }
            return resolved;
        }

        @Override
        public String identity() {
            return owner.getName() + "#<init>" + Arrays.toString(parameterTypes);
        }
    }
}
