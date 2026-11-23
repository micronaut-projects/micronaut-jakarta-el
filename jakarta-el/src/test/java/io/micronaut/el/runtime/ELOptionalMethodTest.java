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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.type.Argument;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELMethod;
import jakarta.el.ELContext;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The method of an optional base object as the {@code OptionalELResolver} of the specification invokes it, for the
 * expressions parsed at runtime. The expression-level behaviour ({@code ${optional.method()}}) is covered with
 * generated twins by the runtime regressions; this checks the Java contract of the method object itself, which
 * has no expression text of its own.
 */
class ELOptionalMethodTest {

    private final ELContext context = new CompiledELContext();

    @Test
    void theMethodOfAnEmptyOptionalIsNullAndDescribesNoParameters() {
        ELMethod method = ELOptionalMethod.of(Optional.empty(), "length", value -> {
            throw new AssertionError("an empty optional has no value to resolve the method on");
        });

        assertEquals("length", method.getName());
        assertSame(Argument.OBJECT_ARGUMENT, method.getReturnType());
        assertArrayEquals(new Argument<?>[0], method.getArguments());
        assertFalse(method.isVarArgs());
        assertSame(AnnotationMetadata.EMPTY_METADATA, method.getAnnotationMetadata());
        assertFalse(method.isReusable());
        assertEquals("java.util.Optional#length", method.identity());
        assertEquals(method.identity(), method.toString());
        assertNull(method.invoke(context, Optional.empty(), new Object[0]));
        assertNull(method.invoke(context, "a value", new Object[0]));
    }

    @Test
    void theMethodOfAPresentOptionalIsTheMethodOfItsValue() {
        LengthMethod length = new LengthMethod();
        ELMethod method = ELOptionalMethod.of(Optional.of("abc"), "length", value -> {
            assertEquals("abc", value);
            return length;
        });

        assertEquals("size", method.getName());
        assertSame(Argument.INT, method.getReturnType());
        assertArrayEquals(new Argument<?>[]{Argument.STRING}, method.getArguments());
        assertTrue(method.isVarArgs());
        assertSame(length.getAnnotationMetadata(), method.getAnnotationMetadata());
        assertFalse(method.isReusable());
        assertEquals("java.util.Optional#" + length.identity(), method.identity());
        // the value the optional holds is the receiver
        assertEquals(3, method.invoke(context, Optional.of("abc"), new Object[0]));
        // a base that is not an optional is the receiver itself
        assertEquals(5, method.invoke(context, "abcde", new Object[0]));
        // an optional that became empty since is null
        assertNull(method.invoke(context, Optional.empty(), new Object[0]));
        assertNull(method.invoke(context, null, new Object[0]));
    }

    /**
     * A method of the value of the optional.
     */
    private static final class LengthMethod implements ELMethod {

        private static final long serialVersionUID = 1L;

        @Override
        public String getName() {
            return "size";
        }

        @Override
        public Argument<?> getReturnType() {
            return Argument.INT;
        }

        @Override
        public Argument<?>[] getArguments() {
            return new Argument<?>[]{Argument.STRING};
        }

        @Override
        public boolean isVarArgs() {
            return true;
        }

        @Override
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            return String.valueOf(base).length();
        }

        @Override
        public String identity() {
            return "java.lang.String#size()";
        }
    }
}
