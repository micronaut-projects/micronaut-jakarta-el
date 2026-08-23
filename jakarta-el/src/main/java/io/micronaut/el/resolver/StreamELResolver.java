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

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.runtime.ELSupport;
import org.jspecify.annotations.Nullable;
import io.micronaut.el.stream.ELOptional;
import io.micronaut.el.stream.ELStream;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import java.util.Collection;

/**
 * The {@link ELResolver} implementing the operations on collection objects described in the chapter 2 of
 * the Jakarta Expression Language specification.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class StreamELResolver extends ELResolver {

    private static final Object[] NO_ARGUMENTS = new Object[0];
    private static final String STREAM = "stream";

    @Override
    @Nullable
    public Object getValue(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, Object base, Object property) {
        return null;
    }

    @Override
    public void setValue(ELContext context, Object base, Object property, Object value) {
        // the stream operations are read only
    }

    @Override
    public boolean isReadOnly(ELContext context, Object base, Object property) {
        return false;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context, Object base, Object method, Class<?>[] paramTypes, Object[] params) {
        if (base == null || method == null) {
            return null;
        }
        Object[] arguments = params == null ? NO_ARGUMENTS : params;
        String name = ELSupport.coerceToString(method);
        if (base instanceof ELStream stream) {
            context.setPropertyResolved(base, method);
            return stream.invokeOperation(name, arguments);
        }
        if (base instanceof ELOptional optional) {
            context.setPropertyResolved(base, method);
            return optional.invokeOperation(context, name, arguments);
        }
        if (STREAM.equals(name) && arguments.length == 0 && isStreamable(base)) {
            context.setPropertyResolved(base, method);
            return ELStream.of(context, base);
        }
        return null;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, Object base) {
        return null;
    }

    private static boolean isStreamable(Object base) {
        return base instanceof Collection<?> || base.getClass().isArray();
    }
}
