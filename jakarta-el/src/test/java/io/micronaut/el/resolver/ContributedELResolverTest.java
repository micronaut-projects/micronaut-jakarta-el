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

import io.micronaut.el.CompiledELContext;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

/**
 * The resolver of the contributed methods only invokes methods, and leaves what its executor does not resolve to
 * the rest of the chain. This is the contract of the resolver API, with no expression text of its own.
 */
class ContributedELResolverTest {

    private final List<Object> resolved = new ArrayList<>();
    private final ContributedELResolver resolver = new ContributedELResolver(
        (context, base, method, argumentTypes, arguments) -> {
            resolved.add(method);
            return null;
        });
    private final CompiledELContext context = new CompiledELContext();

    @Test
    void aNullBaseOrMethodIsNotResolved() {
        assertNull(resolver.invoke(context, null, "length", null, new Object[0]));
        assertNull(resolver.invoke(context, "base", null, null, new Object[0]));
        assertEquals(List.of(), resolved);
        assertFalse(context.isPropertyResolved());
    }

    @Test
    void aMethodTheExecutorDoesNotResolveIsLeftToTheChain() {
        assertNull(resolver.invoke(context, "base", "length", null, null));
        assertEquals(List.of("length"), resolved);
        assertFalse(context.isPropertyResolved());
    }

    @Test
    void itResolvesNoProperty() {
        assertNull(resolver.getValue(context, "base", "length"));
        assertNull(resolver.getType(context, "base", "length"));
        resolver.setValue(context, "base", "length", 1);
        assertFalse(resolver.isReadOnly(context, "base", "length"));
        assertNull(resolver.getCommonPropertyType(context, "base"));
        assertFalse(context.isPropertyResolved());
    }
}
