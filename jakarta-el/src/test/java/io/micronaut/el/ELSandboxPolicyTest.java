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

    private abstract static class AbstractPath implements Path {
    }

    private abstract static class ExtendsAbstractPath extends AbstractPath {
    }

    private static final class SecureLoader extends java.security.SecureClassLoader {
    }
}
