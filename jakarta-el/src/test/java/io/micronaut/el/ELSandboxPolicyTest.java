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

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELSandboxPolicyTest {

    private final ELSandbox sandbox = ELSandbox.standard();

    @Test
    void theFastMemberCheckAgreesWithTheDeclaredList() {
        // allowsMember switches on the length of the name rather than reading the set, because it is asked
        // of every property an expression reads. The set stays the declaration of what is denied.
        for (String denied : ELSandbox.StandardELSandbox.DENIED_MEMBERS) {
            assertFalse(sandbox.allowsMember(Object.class, denied), denied);
        }
        for (String allowed : List.of("title", "name", "get", "getTitle", "value", "getValue", "wai", "waits",
            "clas", "classes", "getClasses", "modules", "notified", "protection", "getProtection", "",
            "notifyAl", "notifyAlls", "getClassLoaders", "classLoade", "getModul", "getProtectionDomains")) {
            assertTrue(sandbox.allowsMember(Object.class, allowed), allowed);
        }
    }

    @Test
    void theResultOfAnExpressionIsOnlyCheckedWhenItCanHoldADeniedType() {
        // a coercion to one of these produces an instance of the target type, and none of them is denied
        for (Class<?> safe : List.of(String.class, Boolean.class, boolean.class, Character.class, char.class,
            Integer.class, int.class, Long.class, Double.class, Number.class, java.math.BigDecimal.class)) {
            assertFalse(ELSandbox.checksResultOf(safe), safe.getName());
        }
        // every other target hands the value back as it is
        for (Class<?> checked : List.of(Object.class, Comparable.class, java.util.List.class, Class.class,
            Object[].class, Runnable.class)) {
            assertTrue(ELSandbox.checksResultOf(checked), checked.getName());
        }
    }
}
