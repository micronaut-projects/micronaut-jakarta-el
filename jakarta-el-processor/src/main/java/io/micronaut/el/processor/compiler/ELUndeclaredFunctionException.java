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
package io.micronaut.el.processor.compiler;

import io.micronaut.core.annotation.Internal;

/**
 * A function that no declaration, and no discovery, provides. The visitor retries the class once every class of
 * the compilation is visited, as the function may be declared in a class not yet seen.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELUndeclaredFunctionException extends ELCompilationException {

    /**
     * @param qualifiedName The qualified name of the function
     */
    public ELUndeclaredFunctionException(String qualifiedName) {
        super("The function '" + qualifiedName + "' is not declared. Declare it with @ELFunction, or list its class with @ELFunctions.");
    }
}
