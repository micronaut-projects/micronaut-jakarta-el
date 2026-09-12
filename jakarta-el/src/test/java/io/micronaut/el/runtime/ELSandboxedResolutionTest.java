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
package io.micronaut.el.runtime;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELSandboxException;
import jakarta.el.BeanELResolver;
import jakarta.el.CompositeELResolver;
import jakarta.el.ELContext;
import jakarta.el.OptionalELResolver;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class ELSandboxedResolutionTest {

    @Test
    void aCompositeOfResolversIsCheckedLikeAResolverThatReflects() {
        // what a composite holds cannot be seen from the chain, so it is checked on the base object it is offered
        // and on the value it produced, whatever resolvers it holds
        CompositeELResolver composite = new CompositeELResolver();
        composite.add(new BeanELResolver());
        ELContext context = new CompiledELContext(composite);
        assertThrows(ELSandboxException.class, () -> ELSandboxedResolution.getValue(context, "hello", "class"));
        assertArrayEquals("hello".getBytes(), (byte[]) ELSandboxedResolution.getValue(context, "hello", "bytes"));
    }

    @Test
    void aSubclassOfTheResolverOfAnOptionalKeepsWhatItOverrides() {
        // only the resolver of the specification is resolved in its place: a subclass may resolve an Optional
        // differently, and is consulted like any resolver the chain does not know
        ELContext context = new CompiledELContext(new LabelledOptionalResolver());
        assertEquals("label", ELSandboxedResolution.getValue(context, Optional.of("hello"), "label"));
    }

    private static final class LabelledOptionalResolver extends OptionalELResolver {

        @Override
        public Object getValue(ELContext context, Object base, Object property) {
            if (base instanceof Optional<?> && "label".equals(property)) {
                context.setPropertyResolved(base, property);
                return "label";
            }
            return super.getValue(context, base, property);
        }
    }
}
