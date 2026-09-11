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
import io.micronaut.el.ELSandbox;
import io.micronaut.el.ELSandboxException;
import io.micronaut.el.resolver.ELResolverChain;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import org.jspecify.annotations.Nullable;

/**
 * The resolution of the properties of an expression parsed at runtime, under the {@link ELSandbox} of its
 * context where the resolution reflects.
 *
 * <p>A chain of this module knows which of its resolvers read the members of a base object reflectively, and
 * consults the sandbox before those and after them only: see {@link ELResolverChain#getValueSandboxed}. What
 * another resolver does cannot be told apart, so a context whose resolver is not such a chain is checked on
 * every property. An expression compiled at compilation time resolves through {@link ELResolution} and is never
 * checked.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
public final class ELSandboxedResolution {

    private ELSandboxedResolution() {
    }

    /**
     * Resolves an identifier as {@link ELResolution#resolveIdentifier} does, reading the field of a static import
     * under the sandbox and checking a class the identifier names, which the import handler loads by name.
     *
     * @param context The context
     * @param name    The identifier
     * @return The value of the identifier
     */
    @Nullable
    public static Object resolveIdentifier(ELContext context, String name) {
        Object value = ELResolution.resolveIdentifier(context, name, true);
        return value instanceof ELClass ? checkValue(ELSandbox.of(context), value) : value;
    }

    /**
     * Resolves a property of a base object as {@link ELResolution#getValue} does.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The resolved value
     */
    @Nullable
    public static Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            return null;
        }
        Object value = resolveValue(context, base, property);
        if (context.isPropertyResolved()) {
            return value;
        }
        throw ELResolution.propertyNotFound(base, property);
    }

    /**
     * Resolves a property of a base object through the resolver of the context, leaving it to
     * {@code propertyResolved} to say whether a resolver resolved it. This is how a resolver that reads the
     * property of a value it holds, as the resolver of an {@link java.util.Optional} does, reaches that property
     * without leaving the sandbox behind.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The resolved value, when a resolver resolved it
     */
    @Nullable
    public static Object resolveValue(ELContext context, Object base, @Nullable Object property) {
        context.setPropertyResolved(false);
        ELResolver resolver = context.getELResolver();
        if (resolver instanceof ELResolverChain chain) {
            return chain.getValueSandboxed(context, base, property);
        }
        ELSandbox sandbox = ELSandbox.of(context);
        checkAccess(sandbox, base, property);
        return checkValue(sandbox, resolver.getValue(context, base, property));
    }

    /**
     * Assigns a value to a property of a base object as {@link ELResolution#setValue} does.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @param value    The value to assign
     */
    public static void setValue(ELContext context,
                                @Nullable Object base,
                                @Nullable Object property,
                                @Nullable Object value) {
        if (base == null || property == null) {
            throw ELResolution.propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        ELResolver resolver = context.getELResolver();
        if (resolver instanceof ELResolverChain chain) {
            chain.setValueSandboxed(context, base, property, value);
        } else {
            checkAccess(ELSandbox.of(context), base, property);
            resolver.setValue(context, base, property, value);
        }
        if (!context.isPropertyResolved()) {
            throw ELResolution.propertyNotFound(base, property);
        }
    }

    /**
     * Assigns a value to a property and returns the assigned value, as {@link ELResolution#assignProperty} does.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @param value    The value
     * @return The assigned value
     */
    @Nullable
    public static Object assignProperty(ELContext context,
                                        @Nullable Object base,
                                        @Nullable Object property,
                                        @Nullable Object value) {
        setValue(context, base, property, value);
        return value;
    }

    /**
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The type of the property, as {@link ELResolution#getType} resolves it
     */
    @Nullable
    public static Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw ELResolution.propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        ELResolver resolver = context.getELResolver();
        Class<?> type;
        if (resolver instanceof ELResolverChain chain) {
            type = chain.getTypeSandboxed(context, base, property);
        } else {
            checkAccess(ELSandbox.of(context), base, property);
            type = resolver.getType(context, base, property);
        }
        if (context.isPropertyResolved()) {
            return type;
        }
        throw ELResolution.propertyNotFound(base, property);
    }

    /**
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return Whether the property is read only, as {@link ELResolution#isReadOnly} resolves it
     */
    public static boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw ELResolution.propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        ELResolver resolver = context.getELResolver();
        boolean readOnly;
        if (resolver instanceof ELResolverChain chain) {
            readOnly = chain.isReadOnlySandboxed(context, base, property);
        } else {
            checkAccess(ELSandbox.of(context), base, property);
            readOnly = resolver.isReadOnly(context, base, property);
        }
        if (context.isPropertyResolved()) {
            return readOnly;
        }
        throw ELResolution.propertyNotFound(base, property);
    }

    /**
     * Fails when the sandbox denies the base object of a reflective access, or the member of it the access
     * names.
     *
     * @param sandbox The sandbox
     * @param base    The base object, an {@link ELClass} for a static member
     * @param member  The property or the method; one that is not a string is a key or an index, not a member
     */
    public static void checkAccess(ELSandbox sandbox, Object base, @Nullable Object member) {
        if (sandbox == ELSandbox.UNRESTRICTED) {
            return;
        }
        Class<?> type = typeOf(base);
        if (!sandbox.allowsType(type)) {
            throw new ELSandboxException(type, null);
        }
        if (member instanceof String name && !sandbox.allowsMember(type, name)) {
            throw new ELSandboxException(type, name);
        }
    }

    /**
     * Fails when the value a reflective access produced is of a type the sandbox denies, so that reflection
     * never hands an expression what it would not let the expression use.
     *
     * @param sandbox The sandbox
     * @param value   The value
     * @param <T>     The type of the value
     * @return The value
     */
    @Nullable
    public static <T> T checkValue(ELSandbox sandbox, @Nullable T value) {
        if (value != null && sandbox != ELSandbox.UNRESTRICTED) {
            Class<?> type = typeOf(value);
            if (!sandbox.allowsType(type)) {
                throw new ELSandboxException(type, null);
            }
        }
        return value;
    }

    private static Class<?> typeOf(Object value) {
        return value instanceof ELClass elClass ? elClass.getKlass() : value.getClass();
    }
}
