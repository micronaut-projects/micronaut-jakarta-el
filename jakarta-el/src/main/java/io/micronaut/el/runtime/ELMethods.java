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
import io.micronaut.core.reflect.ReflectionUtils;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.lang.reflect.Array;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

/**
 * Selects the method a method expression refers to, as described in the section 1.6 of the Jakarta
 * Expression Language specification.
 *
 * <p>When the parameter types are provided at parse time they select the method on their own. Otherwise the
 * evaluated arguments are used: an exact match is preferred over an assignable one, an assignable one over a
 * coercible one, and a method with a fixed arity over a variable arity method. A tie makes the reference
 * ambiguous, which is an error.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELMethods {

    /**
     * The categories a candidate falls in, in the order the section 1.6 of the specification prefers them:
     * a method whose every parameter matches exactly is selected outright, and otherwise the best category
     * holding at least one candidate is reduced to its most specific member.
     */
    private static final int NO_MATCH = 0;
    private static final int EXACT = 1;
    private static final int ASSIGNABLE = 2;
    private static final int COERCIBLE = 3;
    private static final int VARIABLE_ARITY = 4;

    /**
     * The public methods of a class by name, each as its accessible declaration with its parameter types read
     * once: {@code Class.getMethods()} copies every method on every call, which is what makes the reflective
     * resolvers of the specification slow.
     */
    private static final ClassValue<Map<String, List<Candidate>>> METHODS = new ClassValue<>() {
        @Override
        protected Map<String, List<Candidate>> computeValue(Class<?> type) {
            Map<String, List<Candidate>> byName = new HashMap<>();
            for (Method method : type.getMethods()) {
                Method declaration = accessible(method);
                byName.computeIfAbsent(method.getName(), name -> new ArrayList<>(2))
                    .add(new Candidate(declaration, declaration.getParameterTypes(), declaration.isVarArgs(),
                        Modifier.isStatic(declaration.getModifiers()), declaration.isBridge()));
            }
            return byName;
        }
    };

    private ELMethods() {
    }

    /**
     * Finds a method by name and arguments, as {@link #findMethod(Class, String, Class[], Object[])} does, or
     * returns {@code null} when the type declares no public method of the name.
     *
     * @param type       The type of the base object, or the class for a static method
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @param isStatic   Whether to look for a static method
     * @return The method, or {@code null} when the type declares no method of the name
     */
    @Nullable
    public static Method findMethodOrNull(Class<?> type,
                                          String name,
                                          Class<?> @Nullable [] paramTypes,
                                          Object @Nullable [] arguments,
                                          boolean isStatic) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            return null;
        }
        return select(type, name, candidates, paramTypes, arguments, isStatic);
    }

    /**
     * Invokes a method reflectively, the arguments coerced to the parameter types as described in the section
     * 1.23 of the specification, the variable arity arguments packed into an array, the exceptions of the
     * method unwrapped.
     *
     * @param context The context
     * @param method  The method
     * @param base    The base object, or {@code null} for a static method
     * @param values  The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invoke(ELContext context, Method method, @Nullable Object base, Object @Nullable [] values) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] parameters = new Object[parameterTypes.length];
        Object[] arguments = values == null ? new Object[0] : values;
        int count = arguments.length;
        int fixed = method.isVarArgs() ? parameterTypes.length - 1 : parameterTypes.length;
        if (method.isVarArgs() ? count < fixed : count != fixed) {
            throw new IllegalArgumentException("The method '" + method.getName() + "' expects "
                + (method.isVarArgs() ? "at least " + fixed : fixed) + " argument(s) but " + count + " were provided");
        }
        for (int i = 0; i < fixed && i < count; i++) {
            parameters[i] = ELSupport.coerceToType(context, arguments[i], parameterTypes[i]);
        }
        if (method.isVarArgs()) {
            Class<?> componentType = parameterTypes[fixed].getComponentType();
            if (count == parameterTypes.length && arguments[fixed] != null
                && arguments[fixed].getClass() == parameterTypes[fixed]) {
                parameters[fixed] = arguments[fixed];
            } else {
                Object varargs = Array.newInstance(componentType, Math.max(0, count - fixed));
                for (int i = fixed; i < count; i++) {
                    ELArray.set(varargs, i - fixed, ELSupport.coerceToType(context, arguments[i], componentType));
                }
                parameters[fixed] = varargs;
            }
        }
        try {
            return method.invoke(base, parameters);
        } catch (IllegalAccessException | IllegalArgumentException e) {
            throw new ELException(e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            throw cause instanceof ELException elException ? elException : new ELException(cause);
        }
    }

    /**
     * Finds the method a method expression refers to.
     *
     * @param type       The type of the base object
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @return The method
     */
    public static Method findMethod(Class<?> type,
                                    String name,
                                    Class<?> @Nullable [] paramTypes,
                                    Object @Nullable [] arguments) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return select(type, name, candidates, paramTypes, arguments, false);
    }

    /**
     * Finds the static method a method expression refers to.
     *
     * @param type       The type declaring the method
     * @param name       The name of the method
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param arguments  The evaluated arguments, can be {@code null}
     * @return The method
     */
    public static Method findStaticMethod(Class<?> type,
                                          String name,
                                          Class<?> @Nullable [] paramTypes,
                                          Object @Nullable [] arguments) {
        List<Candidate> candidates = METHODS.get(type).get(name);
        if (candidates == null) {
            throw notFound(type, name, paramTypes != null ? paramTypes.length : arguments == null ? 0 : arguments.length);
        }
        return select(type, name, candidates, paramTypes, arguments, true);
    }

    private static Method select(Class<?> type,
                                 String name,
                                 List<Candidate> candidates,
                                 Class<?> @Nullable [] paramTypes,
                                 Object @Nullable [] arguments,
                                 boolean isStatic) {
        Object[] values = arguments == null ? new Object[0] : arguments;
        Class<?>[] matchingTypes = paramTypes == null ? typesOf(values) : paramTypes;
        List<Candidate> assignable = new ArrayList<>(1);
        List<Candidate> coercible = new ArrayList<>(1);
        List<Candidate> variableArity = new ArrayList<>(1);
        for (Candidate candidate : candidates) {
            if (isStatic && !candidate.isStatic()) {
                continue;
            }
            switch (match(candidate, matchingTypes, values)) {
                // a method whose every parameter matches exactly is the method, section 1.6
                case EXACT -> {
                    return candidate.method();
                }
                case ASSIGNABLE -> assignable.add(candidate);
                case COERCIBLE -> coercible.add(candidate);
                case VARIABLE_ARITY -> variableArity.add(candidate);
                default -> {
                    // the candidate does not accept the arguments
                }
            }
        }
        if (!assignable.isEmpty()) {
            return mostSpecific(type, name, assignable, matchingTypes, false);
        }
        if (!coercible.isEmpty()) {
            return mostSpecific(type, name, coercible, matchingTypes, true);
        }
        if (!variableArity.isEmpty()) {
            return mostSpecific(type, name, variableArity, matchingTypes, true);
        }
        throw notFound(type, name, matchingTypes.length);
    }

    /**
     * The category a candidate falls in for the given arguments, as the section 1.6 of the specification
     * classifies it: the worst of its parameters decides, and a variable arity method is only considered once
     * every fixed arity method has been ruled out.
     */
    private static int match(Candidate candidate, Class<?>[] argumentTypes, Object[] values) {
        Class<?>[] parameterTypes = candidate.parameterTypes();
        int parameterCount = parameterTypes.length;
        int argumentCount = argumentTypes.length;
        if (argumentCount != parameterCount && !(candidate.varArgs() && argumentCount >= parameterCount - 1)) {
            return NO_MATCH;
        }
        boolean assignable = false;
        boolean coercible = false;
        boolean varArgs = false;
        for (int i = 0; i < parameterCount; i++) {
            if (i == parameterCount - 1 && candidate.varArgs()) {
                varArgs = true;
                if (parameterCount == argumentCount && parameterTypes[i] == argumentTypes[i]) {
                    continue;
                }
                Class<?> componentType = parameterTypes[i].getComponentType();
                for (int j = i; j < argumentCount; j++) {
                    if (!isAssignable(argumentTypes[j], componentType)
                        && !isCoercible(argumentTypes[j], argument(values, j), componentType)) {
                        return NO_MATCH;
                    }
                }
            } else if (parameterTypes[i].equals(argumentTypes[i])) {
                continue;
            } else if (isAssignable(argumentTypes[i], parameterTypes[i])) {
                assignable = true;
            } else if (isCoercible(argumentTypes[i], argument(values, i), parameterTypes[i])) {
                coercible = true;
            } else {
                return NO_MATCH;
            }
        }
        if (varArgs) {
            return VARIABLE_ARITY;
        }
        if (coercible) {
            return COERCIBLE;
        }
        return assignable ? ASSIGNABLE : EXACT;
    }

    /**
     * Reduces the candidates of one category to the most specific one, as the section 1.6 of the specification
     * and the section 15.12.2.5 of the Java Language Specification define it, leaving the reference ambiguous
     * when two candidates are equally specific.
     */
    private static Method mostSpecific(Class<?> type,
                                       String name,
                                       List<Candidate> candidates,
                                       Class<?>[] argumentTypes,
                                       boolean elSpecific) {
        List<Candidate> best = new ArrayList<>(1);
        for (Candidate candidate : candidates) {
            boolean lessSpecific = false;
            Iterator<Candidate> iterator = best.iterator();
            while (iterator.hasNext()) {
                int comparison = compareSpecificity(candidate, iterator.next(), argumentTypes, elSpecific);
                if (comparison > 0) {
                    iterator.remove();
                } else if (comparison < 0) {
                    lessSpecific = true;
                }
            }
            if (!lessSpecific) {
                best.add(candidate);
            }
        }
        if (best.size() > 1) {
            throw new MethodNotFoundException("The reference to the method '" + name + "' of " + type.getName()
                + " is ambiguous, " + best.size() + " methods match the arguments");
        }
        return best.get(0).method();
    }

    private static int compareSpecificity(Candidate left,
                                          Candidate right,
                                          Class<?>[] argumentTypes,
                                          boolean elSpecific) {
        Class<?>[] leftTypes = left.parameterTypes();
        Class<?>[] rightTypes = right.parameterTypes();
        Class<?>[] matching = argumentTypes;
        if (left.varArgs() || right.varArgs()) {
            int length = Math.max(Math.max(leftTypes.length, rightTypes.length), argumentTypes.length);
            leftTypes = spread(leftTypes, length, left.varArgs());
            rightTypes = spread(rightTypes, length, right.varArgs());
            if (length > argumentTypes.length) {
                matching = Arrays.copyOf(argumentTypes, length);
            }
        }
        if (leftTypes.length != rightTypes.length) {
            return 0;
        }
        int result = 0;
        boolean sameSignature = true;
        for (int i = 0; i < leftTypes.length; i++) {
            if (leftTypes[i] == rightTypes[i]) {
                continue;
            }
            sameSignature = false;
            int comparison = compareSpecificity(leftTypes[i], rightTypes[i], matching[i], elSpecific);
            if (comparison == 0 || (result != 0 && comparison == -result)) {
                return 0;
            }
            result = comparison;
        }
        if (sameSignature) {
            // the same method reached through two supertypes, or a bridge and the method it bridges to:
            // invoking either dispatches to the same implementation, so the reference is not ambiguous. The
            // one that is not a bridge is preferred, and otherwise the first one found is the method.
            int bridge = Boolean.compare(right.bridge(), left.bridge());
            return bridge == 0 ? 1 : bridge;
        }
        return result;
    }

    private static int compareSpecificity(Class<?> left, Class<?> right, @Nullable Class<?> argumentType,
                                          boolean elSpecific) {
        Class<?> boxedLeft = ReflectionUtils.getWrapperType(left);
        Class<?> boxedRight = ReflectionUtils.getWrapperType(right);
        if (boxedRight.isAssignableFrom(boxedLeft)) {
            return 1;
        }
        if (boxedLeft.isAssignableFrom(boxedRight)) {
            return -1;
        }
        // the coercions of the section 1.25 make unrelated types interchangeable, so a numeric argument
        // selects the numeric parameter rather than making the reference ambiguous
        if (elSpecific && argumentType != null && Number.class.isAssignableFrom(argumentType)) {
            boolean leftNumeric = Number.class.isAssignableFrom(boxedLeft);
            boolean rightNumeric = Number.class.isAssignableFrom(boxedRight);
            if (leftNumeric != rightNumeric) {
                return leftNumeric ? 1 : -1;
            }
        }
        return 0;
    }

    /**
     * The parameter types of a candidate as they compare against a call of the given arity, the variable
     * arity parameter repeated for every argument it takes.
     */
    private static Class<?>[] spread(Class<?>[] parameterTypes, int length, boolean varArgs) {
        if (!varArgs) {
            return parameterTypes;
        }
        Class<?>[] spread = new Class<?>[length];
        System.arraycopy(parameterTypes, 0, spread, 0, parameterTypes.length - 1);
        Class<?> componentType = parameterTypes[parameterTypes.length - 1].getComponentType();
        Arrays.fill(spread, parameterTypes.length - 1, length, componentType);
        return spread;
    }

    private static Class<?>[] typesOf(Object[] values) {
        Class<?>[] types = new Class<?>[values.length];
        for (int i = 0; i < values.length; i++) {
            types[i] = values[i] == null ? null : values[i].getClass();
        }
        return types;
    }

    @Nullable
    private static Object argument(Object[] arguments, int index) {
        return index < arguments.length ? arguments[index] : null;
    }

    /**
     * Whether an argument of the given type can be passed as is, a null argument being assignable to
     * anything: the section 1.25 coerces it to the default value of a primitive.
     */
    private static boolean isAssignable(@Nullable Class<?> argumentType, Class<?> parameterType) {
        return argumentType == null
            || ReflectionUtils.getWrapperType(parameterType).isAssignableFrom(argumentType);
    }

    private static boolean isCoercible(@Nullable Class<?> argumentType,
                                       @Nullable Object argument,
                                       Class<?> parameterType) {
        return argumentType == null || argument != null && isCoercible(argument, parameterType);
    }

    /**
     * Whether the coercions of the section 1.25 of the specification can turn the argument into the parameter
     * type. The value decides, not only its type: {@code Boolean.valueOf(1)} has one candidate, not two,
     * because a number is not coercible to a boolean.
     */
    private static boolean isCoercible(@Nullable Object argument, Class<?> parameterType) {
        if (argument == null) {
            return true;
        }
        if (argument instanceof LambdaExpression && parameterType.isInterface()) {
            // the section 1.25.8 of the specification coerces a lambda expression to a functional interface
            return true;
        }
        Class<?> target = ReflectionUtils.getWrapperType(parameterType);
        if (target == String.class) {
            return true;
        }
        if (target == Boolean.class) {
            return argument instanceof String;
        }
        if (target == Character.class) {
            return argument instanceof String || argument instanceof Number;
        }
        if (Number.class.isAssignableFrom(target)) {
            return argument instanceof Number || argument instanceof Character || argument instanceof String;
        }
        return target.isEnum() && argument instanceof String;
    }

    /**
     * Compares declared parameter types with the ones provided at parse time, treating a primitive and its
     * wrapper as the same type: a class literal such as {@code double.class} cannot reach an annotation member
     * of a Micronaut annotation, so the wrapper is what a declaration can provide.
     */
    /**
     * Compares parameter types, treating a primitive and its wrapper as the same type.
     *
     * @param declared The declared parameter types
     * @param provided The parameter types to match
     * @return Whether every declared type matches its provided counterpart
     */
    public static boolean sameTypes(Class<?>[] declared, Class<?>[] provided) {
        if (declared.length != provided.length) {
            return false;
        }
        for (int i = 0; i < declared.length; i++) {
            if (declared[i] != provided[i] && ReflectionUtils.getWrapperType(declared[i]) != ReflectionUtils.getWrapperType(provided[i])) {
                return false;
            }
        }
        return true;
    }

    /**
     * Returns a declaration of the method that can be invoked reflectively.
     *
     * <p>{@code Class.getMethods()} returns the method as declared on the runtime class, which may not be
     * accessible: the lists of {@code List.of}, the views of {@code Collections} and lambdas all implement public
     * interfaces from classes that are not public. The public declaration is found on a supertype, which is
     * what {@code jakarta.el.ELUtil} does for the standard resolvers.</p>
     *
     * @param method The method as declared on the runtime class
     * @return An accessible declaration of the same method
     */
    public static Method accessible(Method method) {
        if (Modifier.isPublic(method.getDeclaringClass().getModifiers())) {
            return method;
        }
        Method found = findAccessible(method.getDeclaringClass(), method);
        return found == null ? method : found;
    }

    @Nullable
    private static Method findAccessible(Class<?> type, Method method) {
        for (Class<?> anInterface : type.getInterfaces()) {
            if (Modifier.isPublic(anInterface.getModifiers())) {
                try {
                    return anInterface.getMethod(method.getName(), method.getParameterTypes());
                } catch (NoSuchMethodException ignored) {
                    // declared further up
                }
            }
            Method found = findAccessible(anInterface, method);
            if (found != null) {
                return found;
            }
        }
        Class<?> superclass = type.getSuperclass();
        if (superclass == null) {
            return null;
        }
        if (Modifier.isPublic(superclass.getModifiers())) {
            try {
                return superclass.getMethod(method.getName(), method.getParameterTypes());
            } catch (NoSuchMethodException ignored) {
                // declared further up
            }
        }
        return findAccessible(superclass, method);
    }

    private static MethodNotFoundException notFound(Class<?> type, String name, int count) {
        return new MethodNotFoundException("Cannot find the method '" + name + "' of " + type.getName()
            + " accepting " + count + " argument(s)");
    }

    /**
     * A public method of a class, with what the selection needs read once.
     *
     * @param method         The accessible declaration of the method
     * @param parameterTypes Its parameter types
     * @param varArgs        Whether it is of variable arity
     * @param isStatic       Whether it is static
     * @param bridge         Whether it is a bridge method, which loses a tie against the method it bridges to
     */
    private record Candidate(Method method, Class<?>[] parameterTypes, boolean varArgs, boolean isStatic,
                            boolean bridge) {
    }
}
