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
import io.micronaut.inject.ast.MethodElement;
import jakarta.el.LambdaExpression;
import org.jspecify.annotations.Nullable;

/**
 * Whether the static type of an argument fits a parameter: the same type, an assignable one, or one the coercion
 * of the section 1.23 of the specification converts, read from the model of the compiler and, for the types of
 * the platform, from their classes.
 */
final class ELTypeCompatibility {

    private ELTypeCompatibility() {
    }

    /**
     * An argument whose static type cannot be coerced to the parameter, section 1.23 of the specification, is
     * an error: the invocation would fail on every evaluation.
     */
    static void requireCoercible(@Nullable ClassElement argument, ClassElement parameter, MethodElement method, int index) {
        if (argument == null || isCoercible(argument, parameter) || mayBeInstance(argument, parameter)) {
            return;
        }
        throw new ELCompilationException("The argument " + (index + 1) + " of "
            + method.getDeclaringType().getName() + "." + method.getName() + " is a " + argument.getName()
            + ", which cannot be coerced to the declared " + parameter.getName()
            + ". Pass a compatible value, or change the signature of the method.");
    }

    /**
     * Whether a value of the type can be coerced to the parameter type: assignable, or one of the coercions of
     * the section 1.23. A string coerces to almost anything, including through a {@code PropertyEditor}, so a
     * string argument is never rejected.
     */
    static boolean isCoercible(ClassElement argument, ClassElement parameter) {
        if (parameter.isArray()) {
            // a variable arity parameter, or an array the runtime coercion of the section 1.23.6 fills
            return true;
        }
        if (ELCompiler.isUnknown(parameter) || ELCompiler.isUnknown(argument)
            || argument.getName().equals(parameter.getName())
            || argument.isAssignable(parameter.getName())
            || argument.getName().equals(String.class.getName())
            || parameter.getName().equals(String.class.getName())
            || parameter.isAssignable(argument.getName())
            || jdkAssignable(argument, parameter)) {
            return true;
        }
        boolean argumentNumeric = numericRank(argument) >= 0 || argument.isAssignable(Number.class)
            || argument.getName().equals("char") || argument.isAssignable(Character.class);
        boolean parameterNumeric = numericRank(parameter) >= 0 || parameter.isAssignable(Number.class)
            || parameter.getName().equals("char") || parameter.isAssignable(Character.class);
        if (argumentNumeric || parameterNumeric) {
            return argumentNumeric && parameterNumeric;
        }
        boolean argumentBoolean = argument.getName().equals("boolean") || argument.isAssignable(Boolean.class);
        boolean parameterBoolean = parameter.getName().equals("boolean") || parameter.isAssignable(Boolean.class);
        if (argumentBoolean || parameterBoolean) {
            return argumentBoolean && parameterBoolean;
        }
        if (parameter.isEnum()) {
            // only a string, handled above, coerces to an enum
            return false;
        }
        if (parameter.isInterface() && argument.isAssignable(LambdaExpression.class)) {
            return true;
        }
        // an unrelated reference type: the coercion of the section 1.23.10 requires an instance of the type
        return false;
    }

    /**
     * Whether a value of the static type of the argument may be an instance of the parameter type at runtime,
     * which the coercion of the section 1.23.10 then passes as it is: the static type of the argument is not
     * the type of the value. An interface and a class that is not final, or two interfaces, may well describe
     * the same instance; only two unrelated classes, or a final class and an interface it does not implement,
     * never do.
     */
    static boolean mayBeInstance(ClassElement argument, ClassElement parameter) {
        if (argument.isArray() || parameter.isArray()) {
            return false;
        }
        Class<?> argumentClass = jdkClass(argument.isPrimitive() ? ELCompiler.wrapper(argument.getName()) : argument.getName());
        Class<?> parameterClass = jdkClass(parameter.getName());
        if (argumentClass != null && parameterClass != null) {
            return parameterClass.isAssignableFrom(argumentClass) || argumentClass.isAssignableFrom(parameterClass)
                || parameterClass.isInterface() && !java.lang.reflect.Modifier.isFinal(argumentClass.getModifiers())
                || argumentClass.isInterface() && !java.lang.reflect.Modifier.isFinal(parameterClass.getModifiers());
        }
        if (argument.isPrimitive()) {
            return false;
        }
        if (parameter.isInterface()) {
            return !argument.isFinal() || argument.isAssignable(parameter.getName());
        }
        return argument.isInterface() && !parameter.isFinal();
    }

    /**
     * Whether a type of the platform, a primitive as its wrapper, is assignable to another type of the platform,
     * read from the classes themselves: the elements of a wrapper built from its class do not always report the
     * interfaces it implements, {@code Comparable} and {@code Serializable} among them.
     */
    static boolean jdkAssignable(ClassElement argument, ClassElement parameter) {
        if (argument.isArray() || parameter.isArray()) {
            return false;
        }
        Class<?> argumentClass = jdkClass(argument.isPrimitive() ? ELCompiler.wrapper(argument.getName()) : argument.getName());
        Class<?> parameterClass = jdkClass(parameter.getName());
        return argumentClass != null && parameterClass != null && !parameterClass.isPrimitive()
            && parameterClass.isAssignableFrom(argumentClass);
    }

    @Nullable
    static Class<?> jdkClass(String name) {
        if (!name.startsWith("java.")) {
            return null;
        }
        try {
            return Class.forName(name, false, ClassLoader.getPlatformClassLoader());
        } catch (ClassNotFoundException | LinkageError e) {
            return null;
        }
    }

    /** Whether two types are the same, an array type being named after its component. */
    static boolean sameType(ClassElement first, ClassElement second) {
        return first.getName().equals(second.getName()) && first.isArray() == second.isArray()
            && first.getArrayDimensions() == second.getArrayDimensions();
    }

    /** Whether an argument is itself the array a variable arity parameter takes. */
    static boolean isArrayArgument(ClassElement argument, ClassElement arrayParameter) {
        if (!argument.isArray() || argument.getArrayDimensions() != arrayParameter.getArrayDimensions()) {
            return false;
        }
        ClassElement component = argument.fromArray();
        ClassElement parameterComponent = arrayParameter.fromArray();
        if (component.getName().equals(parameterComponent.getName())) {
            return true;
        }
        // the array of a subtype is assignable to the array of its supertype, so it is the array the parameter takes,
        // for every kind of invocation: an instance or a static method, a function and a constructor alike
        return !component.isPrimitive() && !parameterComponent.isPrimitive()
            && (component.isAssignable(parameterComponent) || jdkAssignable(component, parameterComponent));
    }

    static boolean isAssignable(ClassElement argument, ClassElement parameter) {
        return argument.isAssignable(parameter)
            || parameter.getName().equals(Object.class.getName())
            || (numericRank(argument) >= 0 && parameter.getName().equals(Number.class.getName()));
    }

    /**
     * @return The width of a numeric type, so that a wider parameter accepts a narrower argument, or -1
     */
    static int numericRank(ClassElement type) {
        return switch (type.getName()) {
            case "byte", ELTypeNames.JAVA_LANG_BYTE -> 0;
            case "short", ELTypeNames.JAVA_LANG_SHORT -> 1;
            case "int", ELTypeNames.JAVA_LANG_INTEGER -> 2;
            case "long", ELTypeNames.JAVA_LANG_LONG -> 3;
            case "float", ELTypeNames.JAVA_LANG_FLOAT -> 4;
            case "double", ELTypeNames.JAVA_LANG_DOUBLE -> 5;
            default -> -1;
        };
    }
}
