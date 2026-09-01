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
package io.micronaut.el.resolver;

import io.micronaut.core.annotation.Internal;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethod;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.runtime.ELArray;
import io.micronaut.el.runtime.ELArguments;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.ELClass;
import jakarta.el.ELException;
import jakarta.el.ELResolver;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import org.jspecify.annotations.Nullable;

import java.util.Collection;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;

/**
 * Resolves the common JDK member operations without reflective invocation.
 *
 * <p>This resolver is intentionally small and only handles operations whose receiver and signature are
 * unambiguous from the runtime type. All other operations continue through the regular resolver chain. It is
 * placed after user resolvers, so applications can still override any of these operations.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class CommonELResolver extends ELResolver implements ELMethodExecutor {

    private static final Object[] NO_ARGUMENTS = new Object[0];

    @Override
    public int getOrder() {
        return 100;
    }

    @Override
    @Nullable
    public Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null) {
            return null;
        }
        int length = ELArray.length(base);
        if (length < 0) {
            return null;
        }
        context.setPropertyResolved(base, property);
        if ("length".equals(property)) {
            return length;
        }
        int index = arrayIndex(property);
        return index >= 0 && index < length ? ELArray.get(base, index) : null;
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base != null && "length".equals(property) && ELArray.isArray(base)) {
            context.setPropertyResolved(base, property);
            return int.class;
        }
        return null;
    }

    @Override
    @Nullable
    public Object invoke(ELContext context,
                         @Nullable Object base,
                         @Nullable Object method,
                         Class<?> @Nullable [] paramTypes,
                         @Nullable Object[] params) {
        ELMethod resolved = resolve(context, base, method, ELArguments.of(paramTypes), params);
        if (resolved == null) {
            return null;
        }
        context.setPropertyResolved(base, method);
        return resolved.invoke(context, base, params);
    }

    @Override
    @Nullable
    public ELMethod resolve(ELContext context,
                            @Nullable Object base,
                            @Nullable Object method,
                            Argument<?> @Nullable [] argumentTypes,
                            Object @Nullable [] arguments) {
        if (base == null || method == null) {
            return null;
        }
        Object[] values = arguments == null ? NO_ARGUMENTS : arguments;
        String name = method.toString();
        return switch (base) {
            case String ignored -> resolveString(name, argumentTypes, values);
            case Collection<?> collection -> resolveCollection(collection, name, argumentTypes, values);
            case Map<?, ?> ignored -> resolveMap(name, argumentTypes, values);
            case ELClass elClass -> resolveStatic(elClass.getKlass(), name, argumentTypes, values);
            default -> null;
        };
    }

    @Override
    public void setValue(ELContext context, @Nullable Object base, @Nullable Object property, @Nullable Object value) {
        if (base == null) {
            return;
        }
        int length = ELArray.length(base);
        if (length < 0) {
            return;
        }
        context.setPropertyResolved(base, property);
        if ("length".equals(property)) {
            throw new PropertyNotWritableException();
        }
        int index = arrayIndex(property);
        if (index < 0 || index >= length) {
            throw new PropertyNotFoundException();
        }
        ELArray.set(base, index, value);
    }

    @Override
    public boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base != null && ELArray.isArray(base)) {
            context.setPropertyResolved(base, property);
            if (!"length".equals(property)) {
                int index = arrayIndex(property);
                if (index < 0 || index >= ELArray.length(base)) {
                    throw new PropertyNotFoundException();
                }
            }
            return "length".equals(property);
        }
        return false;
    }

    @Override
    @Nullable
    public Class<?> getCommonPropertyType(ELContext context, @Nullable Object base) {
        return base != null && ELArray.isArray(base) ? Integer.class : null;
    }

    @Nullable
    private static ELMethod resolveString(String method,
                                          Argument<?> @Nullable [] argumentTypes,
                                          Object[] arguments) {
        int arity = arity(argumentTypes, arguments);
        return switch (method) {
            case "length" -> arity == 0 ? CommonMethod.STRING_LENGTH : null;
            case "isEmpty" -> arity == 0 ? CommonMethod.STRING_IS_EMPTY : null;
            case "toUpperCase" -> arity == 0 ? CommonMethod.STRING_TO_UPPER_CASE : null;
            case "toLowerCase" -> arity == 0 ? CommonMethod.STRING_TO_LOWER_CASE : null;
            case "trim" -> arity == 0 ? CommonMethod.STRING_TRIM : null;
            case "strip" -> arity == 0 ? CommonMethod.STRING_STRIP : null;
            case "stripLeading" -> arity == 0 ? CommonMethod.STRING_STRIP_LEADING : null;
            case "stripTrailing" -> arity == 0 ? CommonMethod.STRING_STRIP_TRAILING : null;
            case "toString" -> arity == 0 ? CommonMethod.STRING_TO_STRING : null;
            case "hashCode" -> arity == 0 ? CommonMethod.STRING_HASH_CODE : null;
            case "substring" -> switch (arity) {
                case 1 -> CommonMethod.STRING_SUBSTRING_ONE;
                case 2 -> CommonMethod.STRING_SUBSTRING_TWO;
                default -> null;
            };
            case "charAt" -> arity == 1 ? CommonMethod.STRING_CHAR_AT : null;
            case "repeat" -> arity == 1 ? CommonMethod.STRING_REPEAT : null;
            case "startsWith" -> arity == 1 ? CommonMethod.STRING_STARTS_WITH : null;
            case "endsWith" -> arity == 1 ? CommonMethod.STRING_ENDS_WITH : null;
            case "contains" -> arity == 1 ? CommonMethod.STRING_CONTAINS : null;
            default -> null;
        };
    }

    @Nullable
    private static ELMethod resolveCollection(Collection<?> collection,
                                              String method,
                                              Argument<?> @Nullable [] argumentTypes,
                                              Object[] arguments) {
        int arity = arity(argumentTypes, arguments);
        return switch (method) {
            case "size" -> arity == 0 ? CommonMethod.COLLECTION_SIZE : null;
            case "isEmpty" -> arity == 0 ? CommonMethod.COLLECTION_IS_EMPTY : null;
            case "contains" -> arity == 1 ? CommonMethod.COLLECTION_CONTAINS : null;
            case "toArray" -> arity == 0 ? CommonMethod.COLLECTION_TO_ARRAY : null;
            case "get" -> collection instanceof List<?> list && arity == 1
                ? CommonMethod.LIST_GET : null;
            case "indexOf" -> collection instanceof List<?> list && arity == 1
                ? CommonMethod.LIST_INDEX_OF : null;
            case "lastIndexOf" -> collection instanceof List<?> list && arity == 1
                ? CommonMethod.LIST_LAST_INDEX_OF : null;
            default -> null;
        };
    }

    @Nullable
    private static ELMethod resolveMap(String method,
                                       Argument<?> @Nullable [] argumentTypes,
                                       Object[] arguments) {
        int arity = arity(argumentTypes, arguments);
        return switch (method) {
            case "size" -> arity == 0 ? CommonMethod.MAP_SIZE : null;
            case "isEmpty" -> arity == 0 ? CommonMethod.MAP_IS_EMPTY : null;
            case "get" -> arity == 1 ? CommonMethod.MAP_GET : null;
            case "containsKey" -> arity == 1 ? CommonMethod.MAP_CONTAINS_KEY : null;
            case "containsValue" -> arity == 1 ? CommonMethod.MAP_CONTAINS_VALUE : null;
            case "keySet" -> arity == 0 ? CommonMethod.MAP_KEY_SET : null;
            case "values" -> arity == 0 ? CommonMethod.MAP_VALUES : null;
            case "entrySet" -> arity == 0 ? CommonMethod.MAP_ENTRY_SET : null;
            default -> null;
        };
    }

    @Nullable
    private static ELMethod resolveStatic(Class<?> type,
                                          String method,
                                          Argument<?> @Nullable [] argumentTypes,
                                          Object[] arguments) {
        int arity = arity(argumentTypes, arguments);
        if (type == Integer.class) {
            return switch (method) {
                case "valueOf" -> arity == 1 && isString(argumentTypes, arguments)
                    ? CommonMethod.INTEGER_VALUE_OF_STRING
                    : arity == 1 ? CommonMethod.INTEGER_VALUE_OF_INT : null;
                case "toHexString" -> arity == 1 ? CommonMethod.INTEGER_TO_HEX_STRING : null;
                default -> null;
            };
        }
        if (type == Math.class && arity == 2) {
            if (arguments.length == 2 && arguments[0] instanceof Integer left && arguments[1] instanceof Integer right) {
                return switch (method) {
                    case "max" -> CommonMethod.MATH_MAX_INT;
                    case "min" -> CommonMethod.MATH_MIN_INT;
                    default -> null;
                };
            }
            if (arguments.length == 2 && arguments[0] instanceof Long left && arguments[1] instanceof Long right) {
                return switch (method) {
                    case "max" -> CommonMethod.MATH_MAX_LONG;
                    case "min" -> CommonMethod.MATH_MIN_LONG;
                    default -> null;
                };
            }
            if (arguments.length == 2 && arguments[0] instanceof Double left && arguments[1] instanceof Double right) {
                return switch (method) {
                    case "max" -> CommonMethod.MATH_MAX_DOUBLE;
                    case "min" -> CommonMethod.MATH_MIN_DOUBLE;
                    default -> null;
                };
            }
            if (arguments.length == 2 && arguments[0] instanceof Float left && arguments[1] instanceof Float right) {
                return switch (method) {
                    case "max" -> CommonMethod.MATH_MAX_FLOAT;
                    case "min" -> CommonMethod.MATH_MIN_FLOAT;
                    default -> null;
                };
            }
            if (argumentTypes != null) {
                Class<?> type0 = argumentTypes[0].getType();
                Class<?> type1 = argumentTypes[1].getType();
                if (isType(type0, int.class) && isType(type1, int.class)) {
                    return "max".equals(method) ? CommonMethod.MATH_MAX_INT
                        : "min".equals(method) ? CommonMethod.MATH_MIN_INT : null;
                }
                if (isType(type0, long.class) && isType(type1, long.class)) {
                    return "max".equals(method) ? CommonMethod.MATH_MAX_LONG
                        : "min".equals(method) ? CommonMethod.MATH_MIN_LONG : null;
                }
                if (isType(type0, double.class) && isType(type1, double.class)) {
                    return "max".equals(method) ? CommonMethod.MATH_MAX_DOUBLE
                        : "min".equals(method) ? CommonMethod.MATH_MIN_DOUBLE : null;
                }
                if (isType(type0, float.class) && isType(type1, float.class)) {
                    return "max".equals(method) ? CommonMethod.MATH_MAX_FLOAT
                        : "min".equals(method) ? CommonMethod.MATH_MIN_FLOAT : null;
                }
            }
        }
        return null;
    }

    private static int arity(Argument<?> @Nullable [] argumentTypes, Object[] arguments) {
        return arguments.length == 0 && argumentTypes != null ? argumentTypes.length : arguments.length;
    }

    private static boolean isString(Argument<?> @Nullable [] argumentTypes, Object[] arguments) {
        return arguments.length == 1 ? arguments[0] instanceof String
            : argumentTypes != null && argumentTypes.length == 1 && argumentTypes[0].getType() == String.class;
    }

    private static boolean isType(Class<?> type, Class<?> expected) {
        return type == expected || type == io.micronaut.core.reflect.ReflectionUtils.getWrapperType(expected);
    }

    private static int arrayIndex(@Nullable Object property) {
        if (property instanceof Integer integer) {
            return integer;
        }
        if (property instanceof Character character) {
            return character;
        }
        if (property instanceof Boolean booleanValue) {
            return booleanValue ? 1 : 0;
        }
        if (property instanceof Number number) {
            return number.intValue();
        }
        if (property instanceof String string) {
            return Integer.parseInt(string);
        }
        throw new IllegalArgumentException();
    }

    private static int intArgument(ELContext context, @Nullable Object value) {
        Integer integer = ELSupport.coerceToType(context, value, int.class);
        if (integer == null) {
            throw new ELException("Cannot coerce a null value to int");
        }
        return integer;
    }

    private enum CommonMethod implements ELMethod {
        STRING_LENGTH("length", int.class),
        STRING_IS_EMPTY("isEmpty", boolean.class),
        STRING_TO_UPPER_CASE("toUpperCase", String.class),
        STRING_TO_LOWER_CASE("toLowerCase", String.class),
        STRING_TRIM("trim", String.class),
        STRING_STRIP("strip", String.class),
        STRING_STRIP_LEADING("stripLeading", String.class),
        STRING_STRIP_TRAILING("stripTrailing", String.class),
        STRING_TO_STRING("toString", String.class),
        STRING_HASH_CODE("hashCode", int.class),
        STRING_SUBSTRING_ONE("substring", String.class, int.class),
        STRING_SUBSTRING_TWO("substring", String.class, int.class, int.class),
        STRING_CHAR_AT("charAt", char.class, int.class),
        STRING_REPEAT("repeat", String.class, int.class),
        STRING_STARTS_WITH("startsWith", boolean.class, String.class),
        STRING_ENDS_WITH("endsWith", boolean.class, String.class),
        STRING_CONTAINS("contains", boolean.class, CharSequence.class),
        COLLECTION_SIZE("size", int.class),
        COLLECTION_IS_EMPTY("isEmpty", boolean.class),
        COLLECTION_CONTAINS("contains", boolean.class, Object.class),
        COLLECTION_TO_ARRAY("toArray", Object[].class),
        LIST_GET("get", Object.class, int.class),
        LIST_INDEX_OF("indexOf", int.class, Object.class),
        LIST_LAST_INDEX_OF("lastIndexOf", int.class, Object.class),
        MAP_SIZE("size", int.class),
        MAP_IS_EMPTY("isEmpty", boolean.class),
        MAP_GET("get", Object.class, Object.class),
        MAP_CONTAINS_KEY("containsKey", boolean.class, Object.class),
        MAP_CONTAINS_VALUE("containsValue", boolean.class, Object.class),
        MAP_KEY_SET("keySet", java.util.Set.class),
        MAP_VALUES("values", Collection.class),
        MAP_ENTRY_SET("entrySet", java.util.Set.class),
        INTEGER_VALUE_OF_STRING("valueOf", Integer.class, String.class),
        INTEGER_VALUE_OF_INT("valueOf", Integer.class, int.class),
        INTEGER_TO_HEX_STRING("toHexString", String.class, int.class),
        MATH_MAX_INT("max", int.class, int.class, int.class),
        MATH_MIN_INT("min", int.class, int.class, int.class),
        MATH_MAX_LONG("max", long.class, long.class, long.class),
        MATH_MIN_LONG("min", long.class, long.class, long.class),
        MATH_MAX_DOUBLE("max", double.class, double.class, double.class),
        MATH_MIN_DOUBLE("min", double.class, double.class, double.class),
        MATH_MAX_FLOAT("max", float.class, float.class, float.class),
        MATH_MIN_FLOAT("min", float.class, float.class, float.class);

        private final String methodName;
        private final Class<?> returnType;
        private final int argumentCount;
        private final Class<?> firstArgument;
        private final Class<?> secondArgument;

        CommonMethod(String name, Class<?> returnType, Class<?>... parameterTypes) {
            if (parameterTypes.length > 2) {
                throw new IllegalArgumentException("Common methods support at most two arguments");
            }
            this.methodName = name;
            this.returnType = returnType;
            this.argumentCount = parameterTypes.length;
            this.firstArgument = parameterTypes.length > 0 ? parameterTypes[0] : void.class;
            this.secondArgument = parameterTypes.length > 1 ? parameterTypes[1] : void.class;
        }

        @Override
        public String getName() {
            return methodName;
        }

        @Override
        public Argument<?> getReturnType() {
            return Argument.of(returnType);
        }

        @Override
        public Argument<?>[] getArguments() {
            return switch (argumentCount) {
                case 0 -> new Argument<?>[0];
                case 1 -> new Argument<?>[]{Argument.of(firstArgument)};
                case 2 -> new Argument<?>[]{Argument.of(firstArgument), Argument.of(secondArgument)};
                default -> throw new IllegalStateException("Unexpected argument count: " + argumentCount);
            };
        }

        @Override
        public boolean isVarArgs() {
            return false;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] arguments) {
            Object[] values = arguments == null ? NO_ARGUMENTS : arguments;
            return switch (this) {
                case STRING_LENGTH -> stringBase(base).length();
                case STRING_IS_EMPTY -> stringBase(base).isEmpty();
                case STRING_TO_UPPER_CASE -> stringBase(base).toUpperCase(Locale.getDefault());
                case STRING_TO_LOWER_CASE -> stringBase(base).toLowerCase(Locale.getDefault());
                case STRING_TRIM -> stringBase(base).trim();
                case STRING_STRIP -> stringBase(base).strip();
                case STRING_STRIP_LEADING -> stringBase(base).stripLeading();
                case STRING_STRIP_TRAILING -> stringBase(base).stripTrailing();
                case STRING_TO_STRING -> stringBase(base).toString();
                case STRING_HASH_CODE -> stringBase(base).hashCode();
                case STRING_SUBSTRING_ONE -> stringBase(base).substring(intArgument(context, values[0]));
                case STRING_SUBSTRING_TWO -> stringBase(base).substring(intArgument(context, values[0]),
                    intArgument(context, values[1]));
                case STRING_CHAR_AT -> stringBase(base).charAt(intArgument(context, values[0]));
                case STRING_REPEAT -> stringBase(base).repeat(intArgument(context, values[0]));
                case STRING_STARTS_WITH -> stringBase(base).startsWith(
                    ELSupport.coerceToType(context, values[0], String.class));
                case STRING_ENDS_WITH -> stringBase(base).endsWith(
                    ELSupport.coerceToType(context, values[0], String.class));
                case STRING_CONTAINS -> stringBase(base).contains(
                    ELSupport.coerceToType(context, values[0], CharSequence.class));
                case COLLECTION_SIZE -> collectionBase(base).size();
                case COLLECTION_IS_EMPTY -> collectionBase(base).isEmpty();
                case COLLECTION_CONTAINS -> collectionBase(base).contains(values[0]);
                case COLLECTION_TO_ARRAY -> collectionBase(base).toArray();
                case LIST_GET -> listBase(base).get(intArgument(context, values[0]));
                case LIST_INDEX_OF -> listBase(base).indexOf(values[0]);
                case LIST_LAST_INDEX_OF -> listBase(base).lastIndexOf(values[0]);
                case MAP_SIZE -> mapBase(base).size();
                case MAP_IS_EMPTY -> mapBase(base).isEmpty();
                case MAP_GET -> mapBase(base).get(values[0]);
                case MAP_CONTAINS_KEY -> mapBase(base).containsKey(values[0]);
                case MAP_CONTAINS_VALUE -> mapBase(base).containsValue(values[0]);
                case MAP_KEY_SET -> mapBase(base).keySet();
                case MAP_VALUES -> mapBase(base).values();
                case MAP_ENTRY_SET -> mapBase(base).entrySet();
                case INTEGER_VALUE_OF_STRING, INTEGER_VALUE_OF_INT -> Integer.valueOf(
                    intArgument(context, values[0]));
                case INTEGER_TO_HEX_STRING -> Integer.toHexString(intArgument(context, values[0]));
                case MATH_MAX_INT -> Math.max((Integer) values[0], (Integer) values[1]);
                case MATH_MIN_INT -> Math.min((Integer) values[0], (Integer) values[1]);
                case MATH_MAX_LONG -> Math.max((Long) values[0], (Long) values[1]);
                case MATH_MIN_LONG -> Math.min((Long) values[0], (Long) values[1]);
                case MATH_MAX_DOUBLE -> Math.max((Double) values[0], (Double) values[1]);
                case MATH_MIN_DOUBLE -> Math.min((Double) values[0], (Double) values[1]);
                case MATH_MAX_FLOAT -> Math.max((Float) values[0], (Float) values[1]);
                case MATH_MIN_FLOAT -> Math.min((Float) values[0], (Float) values[1]);
            };
        }

        private static String stringBase(@Nullable Object base) {
            return (String) Objects.requireNonNull(base);
        }

        private static Collection<?> collectionBase(@Nullable Object base) {
            return (Collection<?>) Objects.requireNonNull(base);
        }

        private static List<?> listBase(@Nullable Object base) {
            return (List<?>) Objects.requireNonNull(base);
        }

        private static Map<?, ?> mapBase(@Nullable Object base) {
            return (Map<?, ?>) Objects.requireNonNull(base);
        }

        @Override
        public String identity() {
            return CommonELResolver.class.getName() + '#' + methodName
                + java.util.Arrays.toString(Argument.toClassArray(getArguments()));
        }
    }
}
