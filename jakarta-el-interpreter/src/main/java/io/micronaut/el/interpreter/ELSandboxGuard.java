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
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSandboxedResolution;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;


/**
 * The resolution of the interpreted expressions, under the {@link ELSandbox} of the context where it reflects.
 *
 * <p>An expression that was compiled at compilation time was written by the developer and resolves through
 * {@link ELResolution} directly. An expression parsed at runtime goes through this class instead, which consults
 * the sandbox only where the resolution reflects: a property through {@link ELSandboxedResolution}, before the
 * resolvers that read it reflectively, a method through the {@link SandboxedELMethod} that wraps what a
 * reflective executor resolved, and a class the expression names, since naming one loads it by name.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
 */
@Internal
final class ELSandboxGuard {

    private ELSandboxGuard() {
    }

    @Nullable
    static Object resolveIdentifier(ELContext context, String name) {
        return ELSandboxedResolution.resolveIdentifier(context, name);
    }

    @Nullable
    static Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        return ELSandboxedResolution.getValue(context, base, property);
    }

    @Nullable
    static Object assignProperty(ELContext context,
                                 @Nullable Object base,
                                 @Nullable Object property,
                                 @Nullable Object value) {
        return ELSandboxedResolution.assignProperty(context, base, property, value);
    }

    static void setValue(ELContext context,
                         @Nullable Object base,
                         @Nullable Object property,
                         @Nullable Object value) {
        ELSandboxedResolution.setValue(context, base, property, value);
    }

    static boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        return ELSandboxedResolution.isReadOnly(context, base, property);
    }

    @Nullable
    static Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        return ELSandboxedResolution.getType(context, base, property);
    }

    /**
     * Fails when the sandbox of the context denies a class an expression named. The import handler loads the
     * class by name, which is reflection before any member of it is reached.
     *
     * @param context The context
     * @param elClass The class
     * @return The class
     */
    static ELClass checkClass(ELContext context, ELClass elClass) {
        ELSandboxedResolution.checkValue(ELSandbox.of(context), elClass);
        return elClass;
    }
}
