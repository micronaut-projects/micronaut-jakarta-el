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
package io.micronaut.el;

import io.micronaut.core.annotation.Experimental;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

/**
 * Raised when an expression parsed at runtime reaches a type or a member its {@link ELSandbox} denies.
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see ELSandbox
 */
@Experimental
public final class ELSandboxException extends ELException {

    private static final long serialVersionUID = 1L;

    private final transient Class<?> type;
    private final transient @Nullable String member;

    /**
     * @param type   The type the expression reached
     * @param member The property or method it reached, {@code null} when the type itself is denied
     */
    public ELSandboxException(Class<?> type, @Nullable String member) {
        super(message(type, member));
        this.type = type;
        this.member = member;
    }

    /**
     * @return The type the expression reached
     */
    public Class<?> getType() {
        return type;
    }

    /**
     * @return The property or method the expression reached, {@code null} when the type itself is denied
     */
    @Nullable
    public String getMember() {
        return member;
    }

    private static String message(Class<?> type, @Nullable String member) {
        String reached = member == null
            ? "the type " + type.getName()
            : "'" + member + "' of " + type.getName();
        return "An expression parsed at runtime is not allowed to reach " + reached
            + ". Declare the expression with @ELExpression so that it is compiled, or widen the sandbox with"
            + " ELContext.putContext(ELSandbox.class, sandbox).";
    }
}
