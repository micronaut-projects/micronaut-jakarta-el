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
import io.micronaut.el.resolver.ELResolverChain;
import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.LambdaExpression;

import java.beans.PropertyEditor;
import java.beans.PropertyEditorManager;
import java.lang.reflect.Array;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.lang.reflect.Proxy;
import java.math.BigDecimal;
import java.math.BigInteger;
import java.util.Collection;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;

/**
 * The type conversion, comparison and emptiness rules of the Jakarta Expression Language specification.
 *
 * <p>This class is invoked by the expressions generated at compilation time, it is not part of the public
 * API of the module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELSupport {

    private ELSupport() {
    }

    /**
     * Coerces a value applying the custom converters of the context before the standard rules.
     *
     * @param context The context, can be {@code null}
     * @param value   The value
     * @param type    The target type
     * @param <T>     The target type
     * @return The coerced value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T coerceToType(@Nullable ELContext context, @Nullable Object value, @Nullable Class<T> type) {
        if (type == null) {
            return coerce(value, null);
        }
        if (value instanceof LambdaExpression && isFunctionalInterface(type)) {
            return coerceToFunctionalInterface(context, value, type);
        }
        if (context != null) {
            ELResolver resolver = context.getELResolver();
            if (resolver instanceof ELResolverChain chain && !chain.converts()) {
                // none of the resolvers converts: the coercion is the one of the specification
                return coerce(value, type);
            }
            if (resolver != null) {
                boolean resolvedSave = context.isPropertyResolved();
                try {
                    context.setPropertyResolved(false);
                    T converted = resolver.convertToType(context, value, type);
                    if (context.isPropertyResolved()) {
                        return converted;
                    }
                } catch (ELException e) {
                    throw e;
                } catch (Exception e) {
                    throw new ELException(e);
                } finally {
                    context.setPropertyResolved(resolvedSave);
                }
            }
        }
        return coerce(value, type);
    }

    /**
     * Coerces a value applying the standard rules of the specification.
     *
     * @param value The value
     * @param type  The target type
     * @param <T>   The target type
     * @return The coerced value
     */
    @SuppressWarnings({"unchecked", "java:S3776"})
    @Nullable
    public static <T> T coerce(@Nullable Object value, @Nullable Class<T> type) {
        if (type != null && value instanceof LambdaExpression && isFunctionalInterface(type)) {
            return coerceToFunctionalInterface(null, value, type);
        }
        if (type == null || type == Object.class) {
            return (T) value;
        }
        if (type == String.class) {
            return (T) coerceToString(value);
        }
        if (type == Boolean.class || type == boolean.class) {
            return (T) coerceToBoolean(value, type == boolean.class);
        }
        if (type == Character.class || type == char.class) {
            return (T) coerceToCharacter(value, type == char.class);
        }
        if (isNumberType(type)) {
            return (T) coerceToNumber(value, type);
        }
        if (type.isEnum()) {
            return (T) coerceToEnum(value, (Class<? extends Enum>) type);
        }
        if (type.isArray()) {
            return (T) coerceToArray(value, type.getComponentType());
        }
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if (value instanceof String string) {
            PropertyEditor editor = PropertyEditorManager.findEditor(type);
            if (editor == null) {
                if (string.isEmpty()) {
                    return null;
                }
                throw cannotCoerce(value, type);
            }
            try {
                editor.setAsText(string);
                return (T) editor.getValue();
            } catch (RuntimeException e) {
                if (string.isEmpty()) {
                    return null;
                }
                throw cannotCoerce(value, type);
            }
        }
        throw cannotCoerce(value, type);
    }

    /**
     * Coerces a value to a functional interface, invoking the lambda expression when one is given.
     *
     * @param context The context
     * @param value   The value
     * @param type    The functional interface
     * @param <T>     The target type
     * @return The coerced value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T> T coerceToFunctionalInterface(@Nullable ELContext context,
                                                    @Nullable Object value,
                                                    @Nullable Class<T> type) {
        if (type != null && value instanceof LambdaExpression lambda && isFunctionalInterface(type)) {
            if (context != null) {
                lambda.setELContext(context);
            }
            return (T) Proxy.newProxyInstance(
                type.getClassLoader(),
                new Class<?>[]{type},
                (proxy, method, args) -> {
                    if (method.getDeclaringClass() == Object.class) {
                        return method.invoke(lambda, args);
                    }
                    Object result = lambda.invoke(args == null ? new Object[0] : args);
                    Class<?> returnType = method.getReturnType();
                    return returnType == void.class ? null : coerceToType(context, result, returnType);
                }
            );
        }
        return coerceToType(context, value, type);
    }

    private static boolean isFunctionalInterface(@Nullable Class<?> type) {
        if (type == null || !type.isInterface()) {
            return false;
        }
        Set<String> abstractMethods = new HashSet<>();
        for (Method method : type.getMethods()) {
            if (Modifier.isAbstract(method.getModifiers()) && !isObjectMethod(method)) {
                abstractMethods.add(method.getName() + java.util.Arrays.toString(method.getParameterTypes()));
            }
        }
        return abstractMethods.size() == 1;
    }

    private static boolean isObjectMethod(Method method) {
        try {
            Object.class.getMethod(method.getName(), method.getParameterTypes());
            return true;
        } catch (NoSuchMethodException e) {
            return false;
        }
    }

    /**
     * Coerces a value to {@link String} as described in the section 1.25.2 of the specification.
     *
     * @param value The value
     * @return The coerced value, never {@code null}
     */
    public static String coerceToString(@Nullable Object value) {
        if (value == null) {
            return "";
        }
        if (value instanceof String string) {
            return string;
        }
        if (value instanceof Enum<?> anEnum) {
            return anEnum.name();
        }
        return value.toString();
    }

    /**
     * Coerces a value to {@link Boolean} as described in the section 1.25.5 of the specification.
     *
     * @param value     The value
     * @param primitive Whether the target type is the primitive {@code boolean}
     * @return The coerced value
     */
    @Nullable
    public static Boolean coerceToBoolean(@Nullable Object value, boolean primitive) {
        if (value instanceof Boolean aBoolean) {
            return aBoolean;
        }
        if (value == null) {
            return primitive ? Boolean.FALSE : null;
        }
        if (value instanceof String string) {
            return string.isEmpty() ? Boolean.FALSE : Boolean.valueOf(string);
        }
        throw cannotCoerce(value, Boolean.class);
    }

    /**
     * Coerces a value to a primitive {@code boolean}, the form used by the logical operators.
     *
     * @param value The value
     * @return The coerced value
     */
    public static boolean toBoolean(@Nullable Object value) {
        Boolean result = coerceToBoolean(value, true);
        return result != null && result;
    }

    /**
     * Coerces a value to {@link Character} as described in the section 1.25.4 of the specification.
     *
     * @param value     The value
     * @param primitive Whether the target type is the primitive {@code char}
     * @return The coerced value
     */
    @Nullable
    public static Character coerceToCharacter(@Nullable Object value, boolean primitive) {
        if (value == null && !primitive) {
            return null;
        }
        if (value == null || "".equals(value)) {
            return (char) 0;
        }
        if (value instanceof Character character) {
            return character;
        }
        if (value instanceof Boolean) {
            throw cannotCoerce(value, Character.class);
        }
        if (value instanceof Number number) {
            return (char) number.shortValue();
        }
        if (value instanceof String string) {
            return string.charAt(0);
        }
        throw cannotCoerce(value, Character.class);
    }

    /**
     * Coerces a value to a number type as described in the section 1.25.3 of the specification.
     *
     * @param value The value
     * @param type  The number type
     * @return The coerced value
     */
    @SuppressWarnings({"unchecked", "java:S3776"})
    @Nullable
    public static Number coerceToNumber(@Nullable Object value, Class<?> type) {
        boolean primitive = type.isPrimitive();
        Class<?> boxed = boxed(type);
        if (value == null) {
            if (!primitive) {
                return null;
            }
            return fromLong(0L, boxed);
        }
        if (value instanceof Character character) {
            return fromNumber((short) character.charValue(), boxed);
        }
        if (value instanceof Boolean) {
            throw cannotCoerce(value, type);
        }
        if (boxed.isInstance(value)) {
            return (Number) value;
        }
        if (value instanceof Number number) {
            return fromNumber(number, boxed);
        }
        if (value instanceof String string) {
            if (string.isEmpty()) {
                return fromLong(0L, boxed);
            }
            try {
                return fromString(string, boxed);
            } catch (NumberFormatException e) {
                throw new ELException("Cannot convert '" + string + "' of type " + String.class.getName()
                    + " to " + type.getName(), e);
            }
        }
        throw cannotCoerce(value, type);
    }

    /**
     * Coerces a value to an enum as described in the section 1.25.6 of the specification.
     *
     * @param value The value
     * @param type  The enum type
     * @param <T>   The enum type
     * @return The coerced value
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public static <T extends Enum<T>> T coerceToEnum(@Nullable Object value, Class<T> type) {
        if (value == null) {
            return null;
        }
        if (type.isInstance(value)) {
            return (T) value;
        }
        if ("".equals(value)) {
            return null;
        }
        if (value instanceof String string) {
            try {
                return Enum.valueOf(type, string);
            } catch (IllegalArgumentException e) {
                throw cannotCoerce(value, type);
            }
        }
        throw cannotCoerce(value, type);
    }

    /**
     * Coerces a value to an array as described in the section 1.25.7 of the specification.
     *
     * @param value         The value
     * @param componentType The component type of the target array
     * @return The coerced value
     */
    @Nullable
    public static Object coerceToArray(@Nullable Object value, Class<?> componentType) {
        if (value == null) {
            return null;
        }
        Class<?> arrayType = Array.newInstance(componentType, 0).getClass();
        if (arrayType.isInstance(value)) {
            return value;
        }
        if (!value.getClass().isArray()) {
            throw cannotCoerce(value, arrayType);
        }
        int length = Array.getLength(value);
        Object result = Array.newInstance(componentType, length);
        for (int i = 0; i < length; i++) {
            Array.set(result, i, coerce(Array.get(value, i), componentType));
        }
        return result;
    }

    /**
     * The {@code empty} operator described in the section 1.11 of the specification.
     *
     * @param value The value
     * @return True if the value is considered empty
     */
    public static boolean isEmpty(@Nullable Object value) {
        if (value == null || "".equals(value)) {
            return true;
        }
        if (value.getClass().isArray()) {
            return Array.getLength(value) == 0;
        }
        if (value instanceof Map<?, ?> map) {
            return map.isEmpty();
        }
        if (value instanceof Collection<?> collection) {
            return collection.isEmpty();
        }
        return false;
    }

    /**
     * @param arguments The arguments
     * @return The number of arguments, zero when the array is {@code null}
     */
    public static int argumentCount(@Nullable Object[] arguments) {
        return arguments == null ? 0 : arguments.length;
    }

    /**
     * @param arguments The arguments
     * @param index     The index of the argument
     * @return The argument at the given index, {@code null} when there is none
     */
    @Nullable
    public static Object argument(@Nullable Object[] arguments, int index) {
        return arguments == null || index >= arguments.length ? null : arguments[index];
    }

    /**
     * The unary {@code not} operator described in the section 1.10.2 of the specification.
     *
     * @param value The operand
     * @return The negated value
     */
    public static boolean not(@Nullable Object value) {
        return !toBoolean(value);
    }

    /**
     * The semicolon operator described in the section 1.14 of the specification. Both operands are
     * evaluated from left to right and the value of the left operand is discarded.
     *
     * @param discarded The value of the left operand
     * @param value     The value of the right operand
     * @return The value of the right operand
     */
    @Nullable
    public static Object sequence(@Nullable Object discarded, @Nullable Object value) {
        return value;
    }

    /**
     * The {@code ==} operator described in the section 1.9.2 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    @SuppressWarnings({"unchecked", "java:S3776"})
    public static boolean equals(@Nullable Object left, @Nullable Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        if (isOperand(left, right, BigDecimal.class)) {
            return number(left, BigDecimal.class).equals(number(right, BigDecimal.class));
        }
        if (isFloatingPointInstance(left, right)) {
            return number(left, Double.class).equals(number(right, Double.class));
        }
        if (isOperand(left, right, BigInteger.class)) {
            return number(left, BigInteger.class).equals(number(right, BigInteger.class));
        }
        if (isWholeNumberOperand(left, right)) {
            return number(left, Long.class).equals(number(right, Long.class));
        }
        if (left instanceof Boolean || right instanceof Boolean) {
            return toBoolean(left) == toBoolean(right);
        }
        if (left instanceof Enum<?> anEnum) {
            return anEnum.equals(coerceToEnum(right, (Class<? extends Enum>) anEnum.getDeclaringClass()));
        }
        if (right instanceof Enum<?> anEnum) {
            return anEnum.equals(coerceToEnum(left, (Class<? extends Enum>) anEnum.getDeclaringClass()));
        }
        if (left instanceof String || right instanceof String) {
            return coerceToString(left).equals(coerceToString(right));
        }
        return left.equals(right);
    }

    /**
     * The {@code !=} operator described in the section 1.9.2 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    public static boolean notEquals(@Nullable Object left, @Nullable Object right) {
        return !equals(left, right);
    }

    /**
     * The {@code <} operator described in the section 1.9.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    public static boolean lessThan(@Nullable Object left, @Nullable Object right) {
        // the section 1.9.1 of the specification: the same object, or a null, is not less than the other
        if (left == right || left == null || right == null) {
            return false;
        }
        return compare(left, right) < 0;
    }

    /**
     * The {@code >} operator described in the section 1.9.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    public static boolean greaterThan(@Nullable Object left, @Nullable Object right) {
        if (left == right || left == null || right == null) {
            return false;
        }
        return compare(left, right) > 0;
    }

    /**
     * The {@code <=} operator described in the section 1.9.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    public static boolean lessThanOrEqual(@Nullable Object left, @Nullable Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right) <= 0;
    }

    /**
     * The {@code >=} operator described in the section 1.9.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return The result of the comparison
     */
    public static boolean greaterThanOrEqual(@Nullable Object left, @Nullable Object right) {
        if (left == right) {
            return true;
        }
        if (left == null || right == null) {
            return false;
        }
        return compare(left, right) >= 0;
    }

    /**
     * Compares two values as described in the section 1.9.1 of the specification.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return A negative value, zero or a positive value
     */
    @SuppressWarnings({"unchecked", "rawtypes", "java:S3776"})
    public static int compare(@Nullable Object left, @Nullable Object right) {
        if (left == right) {
            return 0;
        }
        if (isOperand(left, right, BigDecimal.class)) {
            return ((BigDecimal) number(left, BigDecimal.class)).compareTo((BigDecimal) number(right, BigDecimal.class));
        }
        if (isFloatingPointInstance(left, right)) {
            return Double.compare(doubleValue(left), doubleValue(right));
        }
        if (isOperand(left, right, BigInteger.class)) {
            return ((BigInteger) number(left, BigInteger.class)).compareTo((BigInteger) number(right, BigInteger.class));
        }
        if (isWholeNumberOperand(left, right)) {
            return Long.compare(longValue(left), longValue(right));
        }
        if (left instanceof String || right instanceof String) {
            return coerceToString(left).compareTo(coerceToString(right));
        }
        if (left instanceof Comparable comparable) {
            return comparable.compareTo(right);
        }
        if (right instanceof Comparable comparable) {
            return -comparable.compareTo(left);
        }
        throw new ELException("Cannot compare " + left + " to " + right);
    }

    /**
     * A number as a double, without the boxing of the general coercion when it already is a number.
     */
    private static double doubleValue(@Nullable Object value) {
        return value instanceof Number number ? number.doubleValue() : number(value, Double.class).doubleValue();
    }

    /**
     * A number as a long, without the boxing of the general coercion when it already is a number.
     */
    private static long longValue(@Nullable Object value) {
        return value instanceof Number number ? number.longValue() : number(value, Long.class).longValue();
    }

    /**
     * The floating point rule of the relational operators, which the sections 1.9.1 and 1.9.2 of the
     * specification restrict to actual {@link Float} and {@link Double} instances. Only the arithmetic operators
     * of the section 1.7 extend it to strings in floating point notation.
     *
     * @param left  The left operand
     * @param right The right operand
     * @return True when one of the operands is a Float or a Double
     */
    public static boolean isFloatingPointInstance(@Nullable Object left, @Nullable Object right) {
        return left instanceof Double || left instanceof Float || right instanceof Double || right instanceof Float;
    }

    /**
     * @param left  The left operand
     * @param right The right operand
     * @return True when one of the operands is a floating point value, or a string in floating point notation
     */
    public static boolean isFloatingPointOperand(@Nullable Object left, @Nullable Object right) {
        return isFloatingPoint(left) || isFloatingPoint(right);
    }

    /**
     * @param value The value
     * @return True when the value is a floating point value, including a string in floating point notation
     */
    public static boolean isFloatingPoint(@Nullable Object value) {
        return value instanceof Double || value instanceof Float
            || (value instanceof String string && isFloatingPointNotation(string));
    }

    /**
     * @param value The value
     * @return True when the string uses the floating point notation
     */
    public static boolean isFloatingPointNotation(String value) {
        int length = value.length();
        if (length <= 1) {
            return false;
        }
        for (int i = 0; i < length; i++) {
            char c = value.charAt(i);
            if (c == '.' || c == 'e' || c == 'E') {
                return true;
            }
            if (c != '-' && c != '+' && !Character.isDigit(c)) {
                return false;
            }
        }
        return false;
    }

    /**
     * @param left  The left operand
     * @param right The right operand
     * @param type  The type
     * @return True when one of the operands is of the given type
     */
    public static boolean isOperand(@Nullable Object left, @Nullable Object right, Class<?> type) {
        return type.isInstance(left) || type.isInstance(right);
    }

    private static Number number(@Nullable Object value, Class<?> type) {
        Number number = coerceToNumber(value, type);
        if (number == null) {
            throw cannotCoerce(value, type);
        }
        return number;
    }

    private static boolean isWholeNumberOperand(@Nullable Object left, @Nullable Object right) {
        return isWholeNumber(left) || isWholeNumber(right);
    }

    private static boolean isWholeNumber(@Nullable Object value) {
        return value instanceof Byte || value instanceof Short || value instanceof Character
            || value instanceof Integer || value instanceof Long;
    }

    private static boolean isNumberType(Class<?> type) {
        Class<?> boxed = boxed(type);
        return Number.class.isAssignableFrom(boxed);
    }

    private static Class<?> boxed(Class<?> type) {
        if (!type.isPrimitive()) {
            return type;
        }
        if (type == byte.class) {
            return Byte.class;
        }
        if (type == short.class) {
            return Short.class;
        }
        if (type == int.class) {
            return Integer.class;
        }
        if (type == long.class) {
            return Long.class;
        }
        if (type == float.class) {
            return Float.class;
        }
        if (type == double.class) {
            return Double.class;
        }
        if (type == char.class) {
            return Character.class;
        }
        if (type == boolean.class) {
            return Boolean.class;
        }
        return type;
    }

    private static Number fromNumber(Number value, Class<?> type) {
        if (type == BigInteger.class) {
            if (value instanceof BigDecimal bigDecimal) {
                return bigDecimal.toBigInteger();
            }
            return BigInteger.valueOf(value.longValue());
        }
        if (type == BigDecimal.class) {
            if (value instanceof BigInteger bigInteger) {
                return new BigDecimal(bigInteger);
            }
            return new BigDecimal(value.doubleValue());
        }
        if (type == Byte.class) {
            return value.byteValue();
        }
        if (type == Short.class) {
            return value.shortValue();
        }
        if (type == Integer.class) {
            return value.intValue();
        }
        if (type == Long.class) {
            return value.longValue();
        }
        if (type == Float.class) {
            return value.floatValue();
        }
        if (type == Double.class) {
            return value.doubleValue();
        }
        throw cannotCoerce(value, type);
    }

    private static Number fromLong(long value, Class<?> type) {
        return fromNumber(value, type);
    }

    private static Number fromString(String value, Class<?> type) {
        if (type == BigDecimal.class) {
            return new BigDecimal(value);
        }
        if (type == BigInteger.class) {
            return new BigInteger(value);
        }
        if (type == Byte.class) {
            return Byte.valueOf(value);
        }
        if (type == Short.class) {
            return Short.valueOf(value);
        }
        if (type == Integer.class) {
            return Integer.valueOf(value);
        }
        if (type == Long.class) {
            return Long.valueOf(value);
        }
        if (type == Float.class) {
            return Float.valueOf(value);
        }
        if (type == Double.class) {
            return Double.valueOf(value);
        }
        throw cannotCoerce(value, type);
    }

    private static ELException cannotCoerce(@Nullable Object value, Class<?> type) {
        return new ELException("Cannot convert " + value + " of type "
            + (value == null ? "null" : value.getClass().getName()) + " to " + type.getName());
    }
}
