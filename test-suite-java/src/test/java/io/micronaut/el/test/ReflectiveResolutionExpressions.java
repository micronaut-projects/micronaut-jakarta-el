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
package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

import java.util.Optional;

/**
 * Compile-time counterparts of the interpreted expressions the sandbox of the interpreter checks, because they
 * resolve reflectively when parsed at runtime. Compiled, they are source of the application and never consult it.
 */
@ELEnvironment(
    variables = @ELVariable(name = "optional", type = Optional.class),
    staticImports = Integer.class,
    functions = @ELFunctions(prefix = "rt", value = RuntimeFunctions.class)
)
@ELExpression(value = "${optional.class}", name = "optionalClass")
@ELExpression(value = "${TYPE}", name = "staticImportField")
@ELExpression(value = "${rt:runtime()}", name = "runtimeFunction")
public final class ReflectiveResolutionExpressions {

    private ReflectiveResolutionExpressions() {
    }
}
