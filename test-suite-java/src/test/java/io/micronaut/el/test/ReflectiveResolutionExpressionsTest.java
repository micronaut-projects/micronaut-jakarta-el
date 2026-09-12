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

import io.micronaut.el.CompiledELContext;
import jakarta.el.ELContext;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertSame;

class ReflectiveResolutionExpressionsTest {

    private final ELContext context = new CompiledELContext().setBean("optional", Optional.of("hello"));

    @Test
    void theExpressionsTheInterpreterSandboxesEvaluateWhenCompiled() {
        // ELSandboxTest evaluates the same expressions interpreted: denied under the standard sandbox, and to these
        // values without one
        assertEquals(String.class, ReflectiveResolutionExpressions$ELExpressions.OPTIONAL_CLASS.getValue(context));
        assertEquals(int.class, ReflectiveResolutionExpressions$ELExpressions.STATIC_IMPORT_FIELD.getValue(context));
        assertSame(Runtime.getRuntime(), ReflectiveResolutionExpressions$ELExpressions.RUNTIME_FUNCTION.getValue(context));
    }
}
