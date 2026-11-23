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
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotFoundException;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The assignment the code generated for {@code ${base.property = value}} performs through the resolvers of the
 * context, with a resolver standing for an application one. The resolvers are what the test varies, not the
 * expression, so this exercises the runtime entry point directly; the assignments of the expressions themselves
 * are covered with generated twins by the runtime regressions.
 */
class ELResolutionAssignmentTest {

    private final Target target = new Target();
    private final CompiledELContext context = new CompiledELContext(new TargetResolver());

    @Test
    void theValueIsCoercedToTheTypeTheResolverReports() {
        ELResolution.setValue(context, target, "typed", "42");

        assertEquals(42, target.values.get("typed"));
    }

    @Test
    void theValueIsAssignedAsItIsWhenTheResolverReportsNoType() {
        ELResolution.setValue(context, target, "untyped", "42");

        assertEquals("42", target.values.get("untyped"));
    }

    @Test
    void anIllegalArgumentOfTheResolverIsReportedAsAnELException() {
        ELException failure = assertThrows(ELException.class,
            () -> ELResolution.setValue(context, target, "rejecting", "x"));

        assertInstanceOf(IllegalArgumentException.class, failure.getCause());
    }

    @Test
    void aPropertyNoResolverAssignsIsNotFound() {
        assertThrows(PropertyNotFoundException.class, () -> ELResolution.setValue(context, "base", "missing", 1));
        assertThrows(PropertyNotFoundException.class, () -> ELResolution.setValue(context, null, "missing", 1));
    }

    /**
     * The base object of the resolver.
     */
    private static final class Target {

        private final Map<String, Object> values = new HashMap<>();
    }

    /**
     * Resolves the properties of a {@link Target}: {@code typed} is an Integer, the others have no type the
     * resolver reports, and {@code rejecting} rejects every value.
     */
    private static final class TargetResolver extends ELResolver {

        @Override
        public @Nullable Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
            return null;
        }

        @Override
        public @Nullable Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
            if (!(base instanceof Target)) {
                return null;
            }
            context.setPropertyResolved(base, property);
            return "typed".equals(property) ? Integer.class : null;
        }

        @Override
        public void setValue(ELContext context, @Nullable Object base, @Nullable Object property,
                             @Nullable Object value) {
            if (!(base instanceof Target assigned)) {
                return;
            }
            context.setPropertyResolved(base, property);
            if ("rejecting".equals(property)) {
                throw new IllegalArgumentException("rejected " + value);
            }
            assigned.values.put(String.valueOf(property), value);
        }

        @Override
        public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
            return false;
        }

        @Override
        public @Nullable Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
            return null;
        }
    }
}
