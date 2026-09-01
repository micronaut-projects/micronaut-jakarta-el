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

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;


/**
 * Direct operations on Java arrays.
 *
 * <p>The Jakarta EL array resolver uses {@code java.lang.reflect.Array} for all array element access. These
 * operations are kept in typed branches so interpreted expressions do not pay that reflective invocation cost.</p>
 *
 * @author Denis Stepanov
 */
@Internal
public final class ELArray {

    private static final int NOT_ARRAY = -1;

    private ELArray() {
    }

    /**
     * Returns whether the value is a Java array.
     *
     * @param value The value
     * @return Whether the value is a Java array
     */
    public static boolean isArray(Object value) {
        return length(value) != NOT_ARRAY;
    }

    /**
     * Returns the array length, or {@code -1} when the value is not an array.
     *
     * @param value The value
     * @return The array length, or {@code -1} when the value is not an array
     */
    public static int length(Object value) {
        if (value instanceof Object[] array) {
            return array.length;
        }
        if (value instanceof boolean[] array) {
            return array.length;
        }
        if (value instanceof byte[] array) {
            return array.length;
        }
        if (value instanceof char[] array) {
            return array.length;
        }
        if (value instanceof short[] array) {
            return array.length;
        }
        if (value instanceof int[] array) {
            return array.length;
        }
        if (value instanceof long[] array) {
            return array.length;
        }
        if (value instanceof float[] array) {
            return array.length;
        }
        if (value instanceof double[] array) {
            return array.length;
        }
        return NOT_ARRAY;
    }

    /**
     * Reads an array element using a typed JVM array operation.
     *
     * @param array The array
     * @param index The index
     * @return The element, boxed when the array has a primitive component type
     */
    @Nullable
    public static Object get(Object array, int index) {
        if (array instanceof Object[] values) {
            return values[index];
        }
        if (array instanceof boolean[] values) {
            return values[index];
        }
        if (array instanceof byte[] values) {
            return values[index];
        }
        if (array instanceof char[] values) {
            return values[index];
        }
        if (array instanceof short[] values) {
            return values[index];
        }
        if (array instanceof int[] values) {
            return values[index];
        }
        if (array instanceof long[] values) {
            return values[index];
        }
        if (array instanceof float[] values) {
            return values[index];
        }
        if (array instanceof double[] values) {
            return values[index];
        }
        throw new IllegalArgumentException("Not an array: " + array.getClass().getName());
    }

    /**
     * Writes an array element using a typed JVM array operation.
     *
     * <p>The primitive branches accept the same widening primitive conversions as {@link java.lang.reflect.Array}
     * while reference arrays retain the JVM array-store check.</p>
     *
     * @param array The array
     * @param index The index
     * @param value The value
     */
    public static void set(Object array, int index, @Nullable Object value) {
        if (array instanceof Object[] values) {
            try {
                values[index] = value;
            } catch (ArrayStoreException e) {
                ClassCastException failure = new ClassCastException(e.getMessage());
                failure.initCause(e);
                throw failure;
            }
            return;
        }
        if (array instanceof boolean[] values) {
            values[index] = booleanValue(value);
            return;
        }
        if (array instanceof byte[] values) {
            values[index] = byteValue(value);
            return;
        }
        if (array instanceof char[] values) {
            values[index] = charValue(value);
            return;
        }
        if (array instanceof short[] values) {
            values[index] = shortValue(value);
            return;
        }
        if (array instanceof int[] values) {
            values[index] = intValue(value);
            return;
        }
        if (array instanceof long[] values) {
            values[index] = longValue(value);
            return;
        }
        if (array instanceof float[] values) {
            values[index] = floatValue(value);
            return;
        }
        if (array instanceof double[] values) {
            values[index] = doubleValue(value);
            return;
        }
        throw new IllegalArgumentException("Not an array: " + array.getClass().getName());
    }

    private static boolean booleanValue(@Nullable Object value) {
        if (value instanceof Boolean booleanValue) {
            return booleanValue;
        }
        throw incompatible(value, boolean.class);
    }

    private static byte byteValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        throw incompatible(value, byte.class);
    }

    private static char charValue(@Nullable Object value) {
        if (value instanceof Character character) {
            return character;
        }
        throw incompatible(value, char.class);
    }

    private static short shortValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue;
        }
        throw incompatible(value, short.class);
    }

    private static int intValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue;
        }
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        throw incompatible(value, int.class);
    }

    private static long longValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue;
        }
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        throw incompatible(value, long.class);
    }

    private static float floatValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue;
        }
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Float floatValue) {
            return floatValue;
        }
        throw incompatible(value, float.class);
    }

    private static double doubleValue(@Nullable Object value) {
        if (value instanceof Byte byteValue) {
            return byteValue;
        }
        if (value instanceof Short shortValue) {
            return shortValue;
        }
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Integer integer) {
            return integer;
        }
        if (value instanceof Long longValue) {
            return longValue;
        }
        if (value instanceof Float floatValue) {
            return floatValue;
        }
        if (value instanceof Double doubleValue) {
            return doubleValue;
        }
        throw incompatible(value, double.class);
    }

    private static IllegalArgumentException incompatible(@Nullable Object value, Class<?> targetType) {
        return new IllegalArgumentException("Cannot store "
            + (value == null ? "null" : value.getClass().getName()) + " in " + targetType.getName() + "[]");
    }
}
