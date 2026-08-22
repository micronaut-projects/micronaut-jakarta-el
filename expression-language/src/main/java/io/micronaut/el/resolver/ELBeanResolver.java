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

import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;

import java.util.Set;

/**
 * The reflection free resolution of the properties and the methods of a bean.
 *
 * <p>An implementation is generated at compilation time for every type annotated with
 * {@code io.micronaut.el.annotation.ELBean} and is registered as a service, so that
 * {@link CompiledBeanELResolver} can resolve the type without using reflection.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
public interface ELBeanResolver {

    /**
     * @return The type resolved by this resolver
     */
    Class<?> getBeanType();

    /**
     * @return The names of the resolvable properties
     */
    Set<String> getPropertyNames();

    /**
     * @param name The property name
     * @return The type of the property or {@code null} when the property is unknown
     */
    @Nullable
    Class<?> getPropertyType(String name);

    /**
     * @param name The property name
     * @return True when the property cannot be written
     */
    boolean isReadOnly(String name);

    /**
     * @param context The context
     * @param bean    The bean
     * @param name    The property name
     * @return The value of the property
     */
    @Nullable
    Object getProperty(ELContext context, Object bean, String name);

    /**
     * @param context The context
     * @param bean    The bean
     * @param name    The property name
     * @param value   The value to assign
     */
    void setProperty(ELContext context, Object bean, String name, @Nullable Object value);

    /**
     * @param name      The method name
     * @param arguments The number of arguments
     * @return True when the method can be invoked
     */
    boolean hasMethod(String name, int arguments);

    /**
     * @param context   The context
     * @param bean      The bean
     * @param name      The method name
     * @param arguments The arguments
     * @return The result of the invocation
     */
    @Nullable
    Object invokeMethod(ELContext context, Object bean, String name, Object[] arguments);
}
