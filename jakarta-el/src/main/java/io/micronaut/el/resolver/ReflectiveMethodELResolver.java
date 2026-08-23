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

import io.micronaut.core.annotation.Experimental;
import io.micronaut.el.runtime.ELMethods;
import io.micronaut.el.stream.ELOptional;
import io.micronaut.el.stream.ELStream;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Method;
import java.util.Optional;

/**
 * Invokes the methods of the types that have no bean introspection, reflectively, as {@code BeanELResolver}
 * and {@code StaticFieldELResolver} do, with the method looked up through the cache of {@link ELMethods}:
 * the resolvers of the specification read {@code Class.getMethods()} on every invocation.
 *
 * <p>The resolver only invokes: the properties stay with the resolvers of the specification. It declines the
 * bases the specification gives a resolver of their own for invocation, the streams and the optionals, and
 * the constructors, so that those resolvers keep them.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Experimental
public final class ReflectiveMethodELResolver extends ELResolver {

    private static final String CONSTRUCTOR = "<init>";

    @Override
    @Nullable
    public Object invoke(ELContext context, @Nullable Object base, @Nullable Object method, Class<?> @Nullable [] paramTypes, @Nullable Object[] params) {
        if (base == null || method == null) {
            return null;
        }
        String name = method.toString();
        Method target;
        Object instance;
        if (base instanceof ELClass elClass) {
            if (CONSTRUCTOR.equals(name)) {
                return null;
            }
            target = ELMethods.findMethodOrNull(elClass.getKlass(), name, paramTypes, params, true);
            instance = null;
        } else {
            if (base instanceof ELStream<?> || base instanceof ELOptional<?> || base instanceof Optional<?>) {
                return null;
            }
            target = ELMethods.findMethodOrNull(base.getClass(), name, paramTypes, params, false);
            instance = base;
        }
        if (target == null) {
            return null;
        }
        if (params != null) {
            for (Object param : params) {
                if (param instanceof LambdaExpression lambda) {
                    lambda.setELContext(context);
                }
            }
        }
        Object result = ELMethods.invoke(context, target, instance, params);
        context.setPropertyResolved(base, method);
        return result;
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, @Nullable Object base, @Nullable Object property, @Nullable Object value) {
        // not a property resolver
    }

    @Override
    public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        return false;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return null;
    }
}
