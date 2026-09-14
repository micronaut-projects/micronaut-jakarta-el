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

/**
 * Raised when reflection would work on, or hand an expression parsed at runtime, a type its {@link ELSandbox}
 * denies.
 *
 * @author Denis Stepanov
 * @since 1.1
 * @see ELSandbox
 */
@Experimental
public final class ELSandboxException extends ELException {

    private static final long serialVersionUID = 1L;

    private final transient Class<?> type;

    /**
     * @param type The type the expression reached
     */
    public ELSandboxException(Class<?> type) {
        super("An expression parsed at runtime is not allowed to reach the type " + type.getName()
            + ". Declare the expression with @ELExpression so that it is compiled, or widen the sandbox with"
            + " ELContext.putContext(ELSandbox.class, sandbox).");
        this.type = type;
    }

    /**
     * @return The type the expression reached
     */
    public Class<?> getType() {
        return type;
    }
}
