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
package io.micronaut.el.processor.compiler;

import io.micronaut.inject.ast.ClassElement;
import io.micronaut.sourcegen.model.ClassTypeDef;
import io.micronaut.sourcegen.model.ExpressionDef;
import org.jspecify.annotations.Nullable;

import java.math.BigDecimal;
import java.math.BigInteger;

/**
 * The numeric literals of an expression, section 1.7 of the specification, and their coercion at compilation time.
 */
final class ELLiterals {

    private ELLiterals() {
    }

    /**
     * Coerces a literal to a type at compilation time, as described in the section 1.23 of the specification:
     * a number to a numeric type, a string to a string, a boolean to a boolean.
     *
     * @return The coerced literal, or {@code null} when the value is not a literal the compiler coerces
     */
    @Nullable
    static ExpressionDef coerceLiteral(ExpressionDef value, ClassElement target) {
        if (!(value instanceof ExpressionDef.Constant constant) || constant.value() == null) {
            return null;
        }
        Object literal = constant.value();
        String name = target.getName();
        if (literal instanceof String && name.equals(String.class.getName())
            || literal instanceof Boolean && (name.equals("boolean") || name.equals(Boolean.class.getName()))) {
            return value;
        }
        if (literal instanceof Long || literal instanceof Double) {
            Number number = (Number) literal;
            return switch (name) {
                case "byte" -> ExpressionDef.primitiveConstant(number.byteValue());
                case "short" -> ExpressionDef.primitiveConstant(number.shortValue());
                case "int" -> ExpressionDef.constant(number.intValue());
                case "long" -> ExpressionDef.constant(number.longValue());
                case "float" -> ExpressionDef.constant(number.floatValue());
                case "double" -> ExpressionDef.constant(number.doubleValue());
                case ELTypeNames.JAVA_LANG_BYTE -> ExpressionDef.constant(Byte.valueOf(number.byteValue()));
                case ELTypeNames.JAVA_LANG_SHORT -> ExpressionDef.constant(Short.valueOf(number.shortValue()));
                case ELTypeNames.JAVA_LANG_INTEGER -> ExpressionDef.constant(Integer.valueOf(number.intValue()));
                case ELTypeNames.JAVA_LANG_LONG -> ExpressionDef.constant(Long.valueOf(number.longValue()));
                case ELTypeNames.JAVA_LANG_FLOAT -> ExpressionDef.constant(Float.valueOf(number.floatValue()));
                case ELTypeNames.JAVA_LANG_DOUBLE -> ExpressionDef.constant(Double.valueOf(number.doubleValue()));
                default -> null;
            };
        }
        return null;
    }

    static Class<?> integerLiteralType(String image) {
        try {
            Long.parseLong(image);
            return Long.class;
        } catch (NumberFormatException e) {
            return BigInteger.class;
        }
    }

    static Class<?> floatingPointLiteralType(String image) {
        return Double.isInfinite(Double.parseDouble(image)) ? BigDecimal.class : Double.class;
    }

    static ExpressionDef integerLiteral(String image) {
        try {
            return ExpressionDef.constant(Long.valueOf(image));
        } catch (NumberFormatException e) {
            return ClassTypeDef.of(BigInteger.class).instantiate(ExpressionDef.constant(image));
        }
    }

    static ExpressionDef floatingPointLiteral(String image) {
        double value = Double.parseDouble(image);
        if (Double.isInfinite(value)) {
            return ClassTypeDef.of(BigDecimal.class).instantiate(ExpressionDef.constant(image));
        }
        return ExpressionDef.constant(Double.valueOf(value));
    }
}
