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
import jakarta.el.ELException;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.RoundingMode;

/**
 * The arithmetic and string concatenation operators of the Jakarta Expression Language specification.
 *
 * <p>This class is invoked by the expressions generated at compilation time, it is not part of the public
 * API of the module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELArithmetic {

    private static final Long ZERO = Long.valueOf(0);

    private ELArithmetic() {
    }

    /**
     * The {@code +} operator described in the section 1.7.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result
     */
    public static Object add(@Nullable Object left, @Nullable Object right) {
        if (left == null && right == null) {
            return ZERO;
        }
        if (ELSupport.isOperand(left, right, BigDecimal.class)) {
            return bigDecimal(left).add(bigDecimal(right));
        }
        if (ELSupport.isFloatingPointOperand(left, right)) {
            if (ELSupport.isOperand(left, right, BigInteger.class)) {
                return bigDecimal(left).add(bigDecimal(right));
            }
            return Double.valueOf(doubleValue(left) + doubleValue(right));
        }
        if (ELSupport.isOperand(left, right, BigInteger.class)) {
            return bigInteger(left).add(bigInteger(right));
        }
        return Long.valueOf(longValue(left) + longValue(right));
    }

    /**
     * The {@code -} operator described in the section 1.7.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result
     */
    public static Object subtract(@Nullable Object left, @Nullable Object right) {
        if (left == null && right == null) {
            return ZERO;
        }
        if (ELSupport.isOperand(left, right, BigDecimal.class)) {
            return bigDecimal(left).subtract(bigDecimal(right));
        }
        if (ELSupport.isFloatingPointOperand(left, right)) {
            if (ELSupport.isOperand(left, right, BigInteger.class)) {
                return bigDecimal(left).subtract(bigDecimal(right));
            }
            return Double.valueOf(doubleValue(left) - doubleValue(right));
        }
        if (ELSupport.isOperand(left, right, BigInteger.class)) {
            return bigInteger(left).subtract(bigInteger(right));
        }
        return Long.valueOf(longValue(left) - longValue(right));
    }

    /**
     * The {@code *} operator described in the section 1.7.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result
     */
    public static Object multiply(@Nullable Object left, @Nullable Object right) {
        if (left == null && right == null) {
            return ZERO;
        }
        if (ELSupport.isOperand(left, right, BigDecimal.class)) {
            return bigDecimal(left).multiply(bigDecimal(right));
        }
        if (ELSupport.isFloatingPointOperand(left, right)) {
            if (ELSupport.isOperand(left, right, BigInteger.class)) {
                return bigDecimal(left).multiply(bigDecimal(right));
            }
            return Double.valueOf(doubleValue(left) * doubleValue(right));
        }
        if (ELSupport.isOperand(left, right, BigInteger.class)) {
            return bigInteger(left).multiply(bigInteger(right));
        }
        return Long.valueOf(longValue(left) * longValue(right));
    }

    /**
     * The {@code /} operator described in the section 1.7.2 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result
     */
    public static Object divide(@Nullable Object left, @Nullable Object right) {
        if (left == null && right == null) {
            return ZERO;
        }
        if (ELSupport.isOperand(left, right, BigDecimal.class) || ELSupport.isOperand(left, right, BigInteger.class)) {
            return bigDecimal(left).divide(bigDecimal(right), RoundingMode.HALF_UP);
        }
        return Double.valueOf(doubleValue(left) / doubleValue(right));
    }

    /**
     * The {@code %} operator described in the section 1.7.3 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result
     */
    public static Object mod(@Nullable Object left, @Nullable Object right) {
        if (left == null && right == null) {
            return ZERO;
        }
        if (ELSupport.isOperand(left, right, BigDecimal.class) || ELSupport.isFloatingPointOperand(left, right)) {
            return Double.valueOf(doubleValue(left) % doubleValue(right));
        }
        if (ELSupport.isOperand(left, right, BigInteger.class)) {
            return bigInteger(left).remainder(bigInteger(right));
        }
        return Long.valueOf(longValue(left) % longValue(right));
    }

    /**
     * The unary {@code -} operator described in the section 1.7.4 of the specification.
     *
     * @param value The operand
     * @return The result
     */
    public static Object negate(@Nullable Object value) {
        if (value == null) {
            return ZERO;
        }
        if (value instanceof BigDecimal bigDecimal) {
            return bigDecimal.negate();
        }
        if (value instanceof BigInteger bigInteger) {
            return bigInteger.negate();
        }
        if (value instanceof String string) {
            if (ELSupport.isFloatingPointNotation(string)) {
                return Double.valueOf(-doubleValue(string));
            }
            return Long.valueOf(-longValue(string));
        }
        if (value instanceof Byte aByte) {
            return Byte.valueOf((byte) -aByte.byteValue());
        }
        if (value instanceof Short aShort) {
            return Short.valueOf((short) -aShort.shortValue());
        }
        if (value instanceof Integer anInteger) {
            return Integer.valueOf(-anInteger.intValue());
        }
        if (value instanceof Long aLong) {
            return Long.valueOf(-aLong.longValue());
        }
        if (value instanceof Float aFloat) {
            return Float.valueOf(-aFloat.floatValue());
        }
        if (value instanceof Double aDouble) {
            return Double.valueOf(-aDouble.doubleValue());
        }
        throw new ELException("Cannot apply the unary minus operator to " + value.getClass().getName());
    }

    /**
     * The {@code +=} operator described in the section 1.8 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The concatenated value
     */
    public static String concat(@Nullable Object left, @Nullable Object right) {
        return ELSupport.coerceToString(left) + ELSupport.coerceToString(right);
    }

    private static BigDecimal bigDecimal(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, BigDecimal.class);
        return number == null ? BigDecimal.ZERO : (BigDecimal) number;
    }

    private static BigInteger bigInteger(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, BigInteger.class);
        return number == null ? BigInteger.ZERO : (BigInteger) number;
    }

    private static double doubleValue(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, double.class);
        return number == null ? 0d : number.doubleValue();
    }

    private static long longValue(@Nullable Object value) {
        Number number = ELSupport.coerceToNumber(value, long.class);
        return number == null ? 0L : number.longValue();
    }
}
