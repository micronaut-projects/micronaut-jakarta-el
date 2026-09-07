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
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;

/**
 * Applies the lvalue operations to a resolved target.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class ELResolutionSupport {

    private ELResolutionSupport() {
    }

    static void setIdentifier(ELContext context, @Nullable Object property, @Nullable Object value) {
        ELResolution.setIdentifier(context, ELSupport.coerceToString(property), value);
    }

    static void setValue(ELContext context, ELInterpreter.Target target, @Nullable Object value) {
        ELSandboxGuard.setValue(context, target.base(), target.property(), value);
    }

    static boolean isReadOnly(ELContext context, ELInterpreter.Target target) {
        if (target.base() == null) {
            return ELResolution.isIdentifierReadOnly(context, ELSupport.coerceToString(target.property()));
        }
        return ELSandboxGuard.isReadOnly(context, target.base(), target.property());
    }

    @Nullable
    static Class<?> getType(ELContext context, ELInterpreter.Target target) {
        if (target.base() == null) {
            return ELResolution.getIdentifierType(context, ELSupport.coerceToString(target.property()));
        }
        return ELSandboxGuard.getType(context, target.base(), target.property());
    }
}
