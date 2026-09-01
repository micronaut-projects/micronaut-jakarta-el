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

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ELArrayTest {

    @Test
    void everyJvmArrayKindUsesDirectAccess() {
        Object[] arrays = {
            new Object[]{"object"},
            new boolean[]{true},
            new byte[]{1},
            new char[]{'a'},
            new short[]{2},
            new int[]{3},
            new long[]{4},
            new float[]{5},
            new double[]{6}
        };
        Object[] values = {"object", true, (byte) 1, 'a', (short) 2, 3, 4L, 5F, 6D};

        for (int i = 0; i < arrays.length; i++) {
            assertTrue(ELArray.isArray(arrays[i]));
            assertEquals(1, ELArray.length(arrays[i]));
            assertEquals(values[i], ELArray.get(arrays[i], 0));
        }
        assertFalse(ELArray.isArray("not an array"));
    }

    @Test
    void primitiveArrayWritesUseJvmConversions() {
        Object[] arrays = {
            new Object[1],
            new boolean[1],
            new byte[1],
            new char[1],
            new short[1],
            new int[1],
            new long[1],
            new float[1],
            new double[1]
        };
        Object[] values = {"object", true, (byte) 1, 'a', (byte) 2, (short) 3, 4, 5L, 6F};
        Object[] expected = {"object", true, (byte) 1, 'a', (short) 2, 3, 4L, 5F, 6D};

        for (int i = 0; i < arrays.length; i++) {
            ELArray.set(arrays[i], 0, values[i]);
            assertEquals(expected[i], ELArray.get(arrays[i], 0));
        }
        assertThrows(ClassCastException.class, () -> ELArray.set(new String[1], 0, 1));
    }
}
