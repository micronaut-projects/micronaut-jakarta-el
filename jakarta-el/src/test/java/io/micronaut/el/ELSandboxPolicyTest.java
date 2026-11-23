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

import org.junit.jupiter.api.Test;

import java.nio.file.Path;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELSandboxPolicyTest {

    private final ELSandbox sandbox = ELSandbox.standard();

    @Test
    void aSubtypeOfADeniedTypeIsDeniedWhereverItImplementsIt() {
        // the check compares with the denied types rather than walking the interfaces of the class, so an
        // interface a superclass implements is found as surely as one the class names itself
        assertFalse(sandbox.allowsType(AbstractPath.class));
        assertFalse(sandbox.allowsType(ExtendsAbstractPath.class));
        assertFalse(sandbox.allowsType(SecureLoader.class));
        assertFalse(sandbox.allowsType(ExtendsAbstractPath[][].class));
        assertTrue(sandbox.allowsType(String.class));
        assertTrue(sandbox.allowsType(int[].class));
    }

    @Test
    void aMethodIsDeniedWithItsTypeOrByName() {
        // a method of a denied type is denied whatever its name
        assertFalse(sandbox.allowsMethod(SecureLoader.class, "toString"));
        // the methods reading or changing the global state of an allowed type are denied by name
        assertFalse(sandbox.allowsMethod(Integer.class, "getInteger"));
        assertFalse(sandbox.allowsMethod(java.util.Locale.class, "setDefault"));
        assertTrue(sandbox.allowsMethod(Integer.class, "valueOf"));
        assertTrue(sandbox.allowsMethod(java.util.Locale.class, "getDefault"));
    }

    @Test
    void aSandboxOfTypesAllowsTheMethodsOfTheTypesItAllows() {
        ELSandbox types = type -> type != Integer.class;

        assertTrue(types.allowsMethod(String.class, "length"));
        // the default allows every method of an allowed type, getInteger included
        assertTrue(types.allowsMethod(Long.class, "getLong"));
        assertFalse(types.allowsMethod(Integer.class, "valueOf"));
    }

    private abstract static class AbstractPath implements Path {
    }

    private abstract static class ExtendsAbstractPath extends AbstractPath {
    }

    private static final class SecureLoader extends java.security.SecureClassLoader {
    }
}
