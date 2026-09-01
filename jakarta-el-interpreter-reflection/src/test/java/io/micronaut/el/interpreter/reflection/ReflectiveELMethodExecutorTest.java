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
package io.micronaut.el.interpreter.reflection;

import io.micronaut.el.ELMethod;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELProcessor;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

class ReflectiveELMethodExecutorTest {

    private final ReflectiveELMethodExecutor executor = new ReflectiveELMethodExecutor();
    private final ELContext context = new ELProcessor().getELManager().getELContext();

    @Test
    void resolvesAndInvokesInstanceMethods() {
        ELMethod method = executor.resolve(context, "hello", "toUpperCase", null, new Object[0]);

        assertNotNull(method);
        assertEquals("HELLO", method.invoke(context, "hello", new Object[0]));
    }

    @Test
    void resolvesAndInvokesConstructors() {
        ELMethod method = executor.resolve(context, new ELClass(String.class), "<init>", null,
            new Object[]{"hello"});

        assertNotNull(method);
        assertEquals("hello", method.invoke(context, new ELClass(String.class), new Object[]{"hello"}));
    }
}
