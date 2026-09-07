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
import io.micronaut.el.ELProxyFactory;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.InvocationHandler;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

/**
 * Implements a functional interface nothing described in advance, by discovering its single abstract method
 * and standing in for it with a {@link Proxy}.
 *
 * <p>This is the reflection the coercion of the section 1.23.2 needs when the interface is an application's
 * own and the method taking it was selected at evaluation time, so nothing could name it earlier. It lives
 * here because of that: without this module the coercion does not apply, and an application that wants it
 * without reflection registers the interface with {@code ELMethodRegistry.functionalInterface}.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ReflectiveELProxyFactory implements ELProxyFactory {

    private static final ClassValue<Boolean> FUNCTIONAL_INTERFACES = new ClassValue<>() {
        @Override
        protected Boolean computeValue(Class<?> type) {
            if (!type.isInterface() || type.isAnnotation() || type.isSealed()) {
                return false;
            }
            Set<String> abstractMethods = new HashSet<>();
            for (Method method : type.getMethods()) {
                if (Modifier.isAbstract(method.getModifiers()) && !isObjectMethod(method)) {
                    abstractMethods.add(method.getName() + Arrays.toString(method.getParameterTypes()));
                }
            }
            return abstractMethods.size() == 1;
        }
    };

    @Override
    public boolean isFunctionalInterface(Class<?> type) {
        return FUNCTIONAL_INTERFACES.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T> T create(@Nullable ELContext context, LambdaExpression lambda, Class<T> type) {
        return (T) Proxy.newProxyInstance(
            type.getClassLoader(),
            new Class<?>[]{type},
            (proxy, method, args) -> {
                if (isObjectMethod(method)) {
                    return switch (method.getName()) {
                        case "equals" -> proxy == args[0];
                        case "hashCode" -> System.identityHashCode(proxy);
                        case "toString" -> lambda.toString();
                        default -> throw new IllegalStateException("Unexpected Object method: " + method);
                    };
                }
                if (method.isDefault()) {
                    return InvocationHandler.invokeDefault(proxy, method, args == null ? new Object[0] : args);
                }
                Object[] arguments = args == null ? new Object[0] : args;
                Object result = context == null ? lambda.invoke(arguments) : lambda.invoke(context, arguments);
                Class<?> returnType = method.getReturnType();
                return returnType == void.class ? null : ELSupport.coerceToType(context, result, returnType);
            }
        );
    }

    private static boolean isObjectMethod(Method method) {
        try {
            Object.class.getMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }
}
