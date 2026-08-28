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
package io.micronaut.el.interpreter;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.ELSandboxException;
import io.micronaut.el.runtime.ELResolution;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.function.Supplier;

/**
 * The resolution of the interpreted expressions, under the {@link ELSandbox} of the context.
 *
 * <p>An expression that was compiled at compilation time was written by the developer and resolves through
 * {@link ELResolution} directly. An expression parsed at runtime goes through this class instead, so that
 * every base object it reaches is one the sandbox allows.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELSandboxGuard {

    private ELSandboxGuard() {
    }

    @Nullable
    static Object resolveIdentifier(ELContext context, String name) {
        return checkResolved(context, ELResolution.resolveIdentifier(context, name));
    }

    @Nullable
    static Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        check(context, base, property);
        return ELResolution.getValue(context, base, property);
    }

    @Nullable
    static Object invokeWithParams(ELContext context,
                                   @Nullable Object base,
                                   @Nullable Object method,
                                   @Nullable Object[] params) {
        check(context, base, method);
        return ELResolution.invokeWithParams(context, base, method, params);
    }

    @Nullable
    static Object invokeCallable(ELContext context, @Nullable Object target, Object... arguments) {
        check(context, target, null);
        return ELResolution.invokeCallable(context, target, arguments);
    }

    static Object newInstance(ELContext context, ELClass elClass, Object... arguments) {
        check(context, elClass, null);
        return ELResolution.newInstance(context, elClass, arguments);
    }

    @Nullable
    static Object assignProperty(ELContext context,
                                 @Nullable Object base,
                                 @Nullable Object property,
                                 @Nullable Object value) {
        check(context, base, property);
        return ELResolution.assignProperty(context, base, property, value);
    }

    static void setValue(ELContext context,
                         @Nullable Object base,
                         @Nullable Object property,
                         @Nullable Object value) {
        check(context, base, property);
        ELResolution.setValue(context, base, property, value);
    }

    static boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        check(context, base, property);
        return ELResolution.isReadOnly(context, base, property);
    }

    @Nullable
    static Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        check(context, base, property);
        return ELResolution.getType(context, base, property);
    }

    /**
     * Selects the method a method expression refers to, once the sandbox allows the base object and the name.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The name of the method
     * @param lookup   The selection of the method itself
     * @return The method
     */
    static Method findMethod(ELContext context,
                             @Nullable Object base,
                             @Nullable Object property,
                             Supplier<Method> lookup) {
        check(context, base, property);
        return lookup.get();
    }

    /**
     * Fails when the value an expression is about to hand back is of a type the sandbox denies.
     *
     * <p>An expression reaches a denied type only through a bean of the application that exposes one, since
     * the members that lead to one from any object are denied. It is still not the expression's to return: a
     * caller asking for {@code Object} would receive the {@code Runtime} itself. A caller asking for a
     * {@link String} receives the coercion of it, which is why the check is on the value as coerced.</p>
     *
     * <p>Only the value itself is examined. A denied object the application put inside a collection it
     * exposes is not searched for.</p>
     *
     * @param context The context
     * @param value   The value
     * @param <T>     The type of the value
     * @return The value
     */
    @Nullable
    static <T> T checkResult(ELContext context, @Nullable T value) {
        if (value == null) {
            return null;
        }
        ELSandbox sandbox = ELSandbox.of(context);
        if (sandbox == ELSandbox.UNRESTRICTED) {
            return value;
        }
        Class<?> type = value instanceof ELClass elClass ? elClass.getKlass() : value.getClass();
        if (!sandbox.allowsType(type)) {
            throw new ELSandboxException(type, null);
        }
        return value;
    }

    /**
     * Fails when the sandbox of the context denies the base object, or the member of it the expression names.
     *
     * <p>The check is on the base rather than on the resolved value, so a value of a denied type is only
     * reached, never used: {@code ${bean.getClass()}} fails on the member, and an expression that obtains a
     * {@link Class} some other way fails on the next property it reads from it.</p>
     */
    static void check(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null) {
            return;
        }
        ELSandbox sandbox = ELSandbox.of(context);
        if (sandbox == ELSandbox.UNRESTRICTED) {
            return;
        }
        Class<?> type = base instanceof ELClass elClass ? elClass.getKlass() : base.getClass();
        if (!sandbox.allowsType(type)) {
            throw new ELSandboxException(type, null);
        }
        // a property that is not a string is a key of a map or an index of a list, not a member of the base
        if (property instanceof String member && !sandbox.allowsMember(type, member)) {
            throw new ELSandboxException(type, member);
        }
    }

    /**
     * Fails when an identifier resolves to a class the sandbox denies, which is how an imported class reaches
     * an expression before any member of it is named.
     */
    @Nullable
    private static Object checkResolved(ELContext context, @Nullable Object value) {
        if (value instanceof ELClass elClass) {
            ELSandbox sandbox = ELSandbox.of(context);
            if (sandbox != ELSandbox.UNRESTRICTED && !sandbox.allowsType(elClass.getKlass())) {
                throw new ELSandboxException(elClass.getKlass(), null);
            }
        }
        return value;
    }
}
