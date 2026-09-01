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

import io.micronaut.core.annotation.AnnotationMetadata;
import io.micronaut.core.annotation.Experimental;
import io.micronaut.core.order.Ordered;
import io.micronaut.core.type.Argument;
import io.micronaut.el.runtime.ELMethods;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import org.jspecify.annotations.Nullable;

import java.io.Serializable;
import java.lang.annotation.Annotation;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * Declares the methods, constructors and functions an {@link ELMethodContributor} makes callable from
 * expressions parsed at runtime, and turns them into an {@link ELMethodExecutor} that dispatches them
 * directly, without reflection.
 *
 * <p>A registration carries the declared signature next to the code that runs it. The signature is what the
 * specification needs and what a method reference cannot supply on its own — the overload selection of the
 * section 1.6, the coercions of the section 1.23 and the metadata of a {@code jakarta.el.MethodExpression}
 * are all defined in terms of the declared parameter types:</p>
 *
 * <pre>{@code
 * registry.method(Book.class, "discounted", double.class, Integer.class, Book::discounted)
 *         .staticMethod(Math.class, "abs", int.class, int.class, Math::abs)
 *         .constructor(Book.class, String.class, Book::new);
 * }</pre>
 *
 * <p>The methods a registration produces are reusable: they hold the signature and the invocation, never the
 * arguments they were resolved with, so the interpreter resolves a call site once and re-invokes the same
 * method on every later evaluation. Only a name carrying several overloads is resolved again per call, since
 * the arguments then decide which one applies.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 * @see ELMethodContributor
 */
@Experimental
public final class ELMethodRegistry {

    /**
     * The name a constructor is called by, as the section 1.22 of the specification defines it.
     */
    private static final String CONSTRUCTOR = "<init>";

    private static final Annotation[] NO_ANNOTATIONS = new Annotation[0];
    private static final Object[] NO_ARGUMENTS = new Object[0];
    private static final Class<?>[] NO_PARAMETERS = new Class<?>[0];

    private final Map<Class<?>, Map<String, List<Registration>>> declarations = new LinkedHashMap<>();
    private final Map<String, Registration> functions = new LinkedHashMap<>();

    /**
     * Creates an empty registry.
     */
    public ELMethodRegistry() {
    }

    /**
     * Registers an instance method taking no argument.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param call       The invocation
     * @param <T>        The type declaring the method
     * @param <R>        The return type
     * @return This registry
     */
    public <T, R> ELMethodRegistry method(Class<T> type, String name, Class<R> returnType, Call0<T, R> call) {
        return method(type, name, returnType, NO_PARAMETERS, false,
            (context, base, arguments) -> call.call(receiver(base)));
    }

    /**
     * Registers an instance method taking one argument.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param first      The declared type of the argument
     * @param call       The invocation
     * @param <T>        The type declaring the method
     * @param <A>        The type of the argument
     * @param <R>        The return type
     * @return This registry
     */
    public <T, A, R> ELMethodRegistry method(Class<T> type,
                                             String name,
                                             Class<R> returnType,
                                             Class<A> first,
                                             Call1<T, A, R> call) {
        return method(type, name, returnType, new Class<?>[]{first}, false,
            (context, base, arguments) -> call.call(receiver(base), cast(arguments[0])));
    }

    /**
     * Registers an instance method taking two arguments.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param first      The declared type of the first argument
     * @param second     The declared type of the second argument
     * @param call       The invocation
     * @param <T>        The type declaring the method
     * @param <A>        The type of the first argument
     * @param <B>        The type of the second argument
     * @param <R>        The return type
     * @return This registry
     */
    public <T, A, B, R> ELMethodRegistry method(Class<T> type,
                                                String name,
                                                Class<R> returnType,
                                                Class<A> first,
                                                Class<B> second,
                                                Call2<T, A, B, R> call) {
        return method(type, name, returnType, new Class<?>[]{first, second}, false,
            (context, base, arguments) -> call.call(receiver(base), cast(arguments[0]), cast(arguments[1])));
    }

    /**
     * Registers an instance method of any arity, including one of variable arity.
     *
     * <p>The arguments reach the invocation coerced to {@code parameterTypes}, the variable arity ones packed
     * into an array of the component type of the last parameter.</p>
     *
     * @param type           The type declaring the method
     * @param name           The name the expression calls it by
     * @param returnType     The declared return type
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity, which requires it to be an array
     * @param invocation     The invocation
     * @return This registry
     */
    public ELMethodRegistry method(Class<?> type,
                                   String name,
                                   Class<?> returnType,
                                   Class<?>[] parameterTypes,
                                   boolean varArgs,
                                   Invocation invocation) {
        return method(type, name, returnType, parameterTypes, varArgs, NO_ANNOTATIONS, invocation);
    }

    /**
     * Registers an instance method carrying annotations, which a {@code jakarta.el.MethodExpression} reports
     * through {@code jakarta.el.MethodReference}.
     *
     * @param type           The type declaring the method
     * @param name           The name the expression calls it by
     * @param returnType     The declared return type
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity, which requires it to be an array
     * @param annotations    The annotations declared on the method
     * @param invocation     The invocation
     * @return This registry
     */
    public ELMethodRegistry method(Class<?> type,
                                   String name,
                                   Class<?> returnType,
                                   Class<?>[] parameterTypes,
                                   boolean varArgs,
                                   Annotation[] annotations,
                                   Invocation invocation) {
        return declare(type, name, new Registration(type, name, returnType, parameterTypes.clone(), varArgs,
            false, annotations.clone(), invocation));
    }

    /**
     * Registers a static method taking no argument.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param call       The invocation
     * @param <R>        The return type
     * @return This registry
     */
    public <R> ELMethodRegistry staticMethod(Class<?> type, String name, Class<R> returnType, Fn0<R> call) {
        return staticMethod(type, name, returnType, NO_PARAMETERS, false,
            (context, base, arguments) -> call.call());
    }

    /**
     * Registers a static method taking one argument.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param first      The declared type of the argument
     * @param call       The invocation
     * @param <A>        The type of the argument
     * @param <R>        The return type
     * @return This registry
     */
    public <A, R> ELMethodRegistry staticMethod(Class<?> type,
                                                String name,
                                                Class<R> returnType,
                                                Class<A> first,
                                                Fn1<A, R> call) {
        return staticMethod(type, name, returnType, new Class<?>[]{first}, false,
            (context, base, arguments) -> call.call(cast(arguments[0])));
    }

    /**
     * Registers a static method taking two arguments.
     *
     * @param type       The type declaring the method
     * @param name       The name the expression calls it by
     * @param returnType The declared return type
     * @param first      The declared type of the first argument
     * @param second     The declared type of the second argument
     * @param call       The invocation
     * @param <A>        The type of the first argument
     * @param <B>        The type of the second argument
     * @param <R>        The return type
     * @return This registry
     */
    public <A, B, R> ELMethodRegistry staticMethod(Class<?> type,
                                                   String name,
                                                   Class<R> returnType,
                                                   Class<A> first,
                                                   Class<B> second,
                                                   Fn2<A, B, R> call) {
        return staticMethod(type, name, returnType, new Class<?>[]{first, second}, false,
            (context, base, arguments) -> call.call(cast(arguments[0]), cast(arguments[1])));
    }

    /**
     * Registers a static method of any arity, including one of variable arity.
     *
     * @param type           The type declaring the method
     * @param name           The name the expression calls it by
     * @param returnType     The declared return type
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity, which requires it to be an array
     * @param invocation     The invocation, which receives a {@code null} base
     * @return This registry
     */
    public ELMethodRegistry staticMethod(Class<?> type,
                                         String name,
                                         Class<?> returnType,
                                         Class<?>[] parameterTypes,
                                         boolean varArgs,
                                         Invocation invocation) {
        return declare(type, name, new Registration(type, name, returnType, parameterTypes.clone(), varArgs,
            true, NO_ANNOTATIONS, invocation));
    }

    /**
     * Registers a constructor taking one argument, callable as {@code Type(argument)}.
     *
     * @param type  The type to construct
     * @param first The declared type of the argument
     * @param call  The invocation
     * @param <A>   The type of the argument
     * @param <T>   The type to construct
     * @return This registry
     */
    public <A, T> ELMethodRegistry constructor(Class<T> type, Class<A> first, Fn1<A, T> call) {
        return constructor(type, new Class<?>[]{first}, false,
            (context, base, arguments) -> call.call(cast(arguments[0])));
    }

    /**
     * Registers a constructor of any arity, callable as {@code Type(arguments)}.
     *
     * @param type           The type to construct
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity, which requires it to be an array
     * @param invocation     The invocation, which receives a {@code null} base
     * @return This registry
     */
    public ELMethodRegistry constructor(Class<?> type,
                                        Class<?>[] parameterTypes,
                                        boolean varArgs,
                                        Invocation invocation) {
        return declare(type, CONSTRUCTOR, new Registration(type, CONSTRUCTOR, type,
            parameterTypes.clone(), varArgs, true, NO_ANNOTATIONS, invocation));
    }

    /**
     * Registers a function, callable as {@code prefix:localName(arguments)}, or as {@code localName(arguments)}
     * when the prefix is empty.
     *
     * <p>A function registered here replaces the {@code jakarta.el.FunctionMapper} lookup, which is defined in
     * terms of {@code java.lang.reflect.Method} and therefore needs reflection.</p>
     *
     * @param prefix         The namespace prefix, empty for a function without one
     * @param localName      The name within the namespace
     * @param declaringType  The type declaring the method, which the identity of the expression is built from
     * @param methodName     The name of the method, which the metadata of the expression reports
     * @param returnType     The declared return type
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity, which requires it to be an array
     * @param invocation     The invocation, which receives a {@code null} base
     * @return This registry
     */
    public ELMethodRegistry function(String prefix,
                                     String localName,
                                     Class<?> declaringType,
                                     String methodName,
                                     Class<?> returnType,
                                     Class<?>[] parameterTypes,
                                     boolean varArgs,
                                     Invocation invocation) {
        Registration registration = new Registration(declaringType, methodName, returnType,
            parameterTypes.clone(), varArgs, true, NO_ANNOTATIONS, invocation);
        validate(declaringType, methodName, registration);
        functions.put(qualifiedName(prefix, localName), registration);
        return this;
    }

    /**
     * Registers a function taking one argument.
     *
     * @param prefix        The namespace prefix, empty for a function without one
     * @param localName     The name within the namespace
     * @param declaringType The type declaring the method, which the identity of the expression is built from
     * @param methodName    The name of the method, which the metadata of the expression reports
     * @param returnType    The declared return type
     * @param first         The declared type of the argument
     * @param call          The invocation
     * @param <A>           The type of the argument
     * @param <R>           The return type
     * @return This registry
     */
    public <A, R> ELMethodRegistry function(String prefix,
                                            String localName,
                                            Class<?> declaringType,
                                            String methodName,
                                            Class<R> returnType,
                                            Class<A> first,
                                            Fn1<A, R> call) {
        return function(prefix, localName, declaringType, methodName, returnType, new Class<?>[]{first}, false,
            (context, base, arguments) -> call.call(cast(arguments[0])));
    }

    /**
     * Builds the executor dispatching everything registered so far.
     *
     * <p>The registrations are indexed by type and name, and the methods a type inherits from its supertypes
     * and interfaces are merged into it, so a method registered on an interface is callable on every
     * implementation. A constructor is not inherited, since it constructs the type it was registered for. The
     * executor is immutable; later registrations do not reach it.</p>
     *
     * @param order The order of the executor among the others, see {@link Ordered}
     * @return The executor
     */
    public ELMethodExecutor build(int order) {
        Map<Class<?>, Map<String, List<Registration>>> snapshot = new HashMap<>();
        declarations.forEach((type, byName) -> {
            Map<String, List<Registration>> copy = new HashMap<>();
            byName.forEach((name, list) -> copy.put(name, List.copyOf(list)));
            snapshot.put(type, copy);
        });
        return new RegisteredMethodExecutor(snapshot, Map.copyOf(functions), order);
    }

    /**
     * Whether nothing has been registered.
     *
     * @return Whether the registry is empty
     */
    public boolean isEmpty() {
        return declarations.isEmpty() && functions.isEmpty();
    }

    private ELMethodRegistry declare(Class<?> type, String name, Registration registration) {
        validate(type, name, registration);
        declarations.computeIfAbsent(type, ignored -> new LinkedHashMap<>())
            .computeIfAbsent(name, ignored -> new ArrayList<>(2))
            .add(registration);
        return this;
    }

    private static void validate(Class<?> type, String name, Registration registration) {
        if (registration.varArgs()
            && (registration.parameterTypes().length == 0
                || !registration.parameterTypes()[registration.parameterTypes().length - 1].isArray())) {
            throw new IllegalArgumentException("The variable arity method '" + name + "' of " + type.getName()
                + " must declare an array as its last parameter");
        }
    }

    private static String qualifiedName(String prefix, String localName) {
        return prefix.isEmpty() ? localName : prefix + ":" + localName;
    }

    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private static <T> @Nullable T cast(@Nullable Object value) {
        return (T) value;
    }

    /**
     * The receiver of an instance method, which the resolution guarantees is not null.
     */
    @SuppressWarnings({"unchecked", "TypeParameterUnusedInFormals"})
    private static <T> T receiver(@Nullable Object base) {
        return (T) Objects.requireNonNull(base, "The instance of an instance method cannot be null");
    }

    /**
     * Invokes a registered method of any arity.
     */
    @FunctionalInterface
    public interface Invocation extends Serializable {

        /**
         * Invokes the method.
         *
         * @param context   The context
         * @param base      The instance, or {@code null} for a static method, a constructor or a function
         * @param arguments The arguments, coerced to the declared parameter types
         * @return The result
         */
        @Nullable
        Object invoke(ELContext context, @Nullable Object base, Object[] arguments);
    }

    /**
     * Invokes an instance method taking no argument.
     *
     * @param <T> The type declaring the method
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Call0<T, R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @param base The instance
         * @return The result
         */
        R call(T base);
    }

    /**
     * Invokes an instance method taking one argument.
     *
     * @param <T> The type declaring the method
     * @param <A> The type of the argument
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Call1<T, A, R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @param base  The instance
         * @param first The argument
         * @return The result
         */
        R call(T base, @Nullable A first);
    }

    /**
     * Invokes an instance method taking two arguments.
     *
     * @param <T> The type declaring the method
     * @param <A> The type of the first argument
     * @param <B> The type of the second argument
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Call2<T, A, B, R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @param base   The instance
         * @param first  The first argument
         * @param second The second argument
         * @return The result
         */
        R call(T base, @Nullable A first, @Nullable B second);
    }

    /**
     * Invokes a static method, a constructor or a function taking no argument.
     *
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Fn0<R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @return The result
         */
        R call();
    }

    /**
     * Invokes a static method, a constructor or a function taking one argument.
     *
     * @param <A> The type of the argument
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Fn1<A, R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @param first The argument
         * @return The result
         */
        R call(@Nullable A first);
    }

    /**
     * Invokes a static method, a constructor or a function taking two arguments.
     *
     * @param <A> The type of the first argument
     * @param <B> The type of the second argument
     * @param <R> The return type
     */
    @FunctionalInterface
    public interface Fn2<A, B, R> extends Serializable {

        /**
         * Invokes the method.
         *
         * @param first  The first argument
         * @param second The second argument
         * @return The result
         */
        R call(@Nullable A first, @Nullable B second);
    }

    /**
     * A declared signature and the code that runs it.
     *
     * @param declaringType  The type declaring the method
     * @param name           The name of the method
     * @param returnType     The declared return type
     * @param parameterTypes The declared parameter types
     * @param varArgs        Whether the last parameter is of variable arity
     * @param isStatic       Whether the method is static, a constructor or a function
     * @param annotations    The annotations declared on the method
     * @param invocation     The code that runs it
     */
    @SuppressWarnings("ArrayRecordComponent")
    private record Registration(Class<?> declaringType,
                                String name,
                                Class<?> returnType,
                                Class<?>[] parameterTypes,
                                boolean varArgs,
                                boolean isStatic,
                                Annotation[] annotations,
                                Invocation invocation) implements Serializable {

        private static final long serialVersionUID = 1L;
    }

    /**
     * A method resolved from a registration.
     *
     * <p>It holds the signature and the invocation only, never the arguments it was resolved with, so the
     * interpreter can keep it in the evaluator of the call site and invoke it again.</p>
     */
    private static final class RegisteredMethod implements ELMethod {

        private static final long serialVersionUID = 1L;

        private final Registration registration;
        private final boolean reusable;
        private transient Argument<?> @Nullable [] arguments;
        private transient @Nullable Argument<?> returnType;

        private RegisteredMethod(Registration registration, boolean reusable) {
            this.registration = registration;
            this.reusable = reusable;
        }

        @Override
        public String getName() {
            return registration.name();
        }

        @Override
        public Argument<?> getReturnType() {
            Argument<?> resolved = returnType;
            if (resolved == null) {
                resolved = Argument.of(registration.returnType());
                returnType = resolved;
            }
            return resolved;
        }

        @Override
        public Argument<?>[] getArguments() {
            Argument<?>[] resolved = arguments;
            if (resolved == null) {
                Class<?>[] parameterTypes = registration.parameterTypes();
                resolved = new Argument<?>[parameterTypes.length];
                for (int i = 0; i < parameterTypes.length; i++) {
                    resolved[i] = Argument.of(parameterTypes[i]);
                }
                arguments = resolved;
            }
            return resolved;
        }

        @Override
        public boolean isVarArgs() {
            return registration.varArgs();
        }

        @Override
        public AnnotationMetadata getAnnotationMetadata() {
            return AnnotationMetadata.EMPTY_METADATA;
        }

        @Override
        public Annotation[] synthesizeAnnotations() {
            return registration.annotations().clone();
        }

        @Override
        public boolean isReusable() {
            return reusable;
        }

        @Override
        @Nullable
        public Object invoke(ELContext context, @Nullable Object base, Object @Nullable [] values) {
            Object[] parameters = ELMethods.coerceArguments(context, registration.name(),
                registration.parameterTypes(), registration.varArgs(), values);
            Object target = registration.isStatic() || base instanceof ELClass ? null : base;
            try {
                return registration.invocation().invoke(context, target, parameters);
            } catch (ELException e) {
                throw e;
            } catch (RuntimeException e) {
                // what the method itself threw reaches the caller of the expression as an ELException, the
                // way the specification reports the exception of a reflectively invoked method
                throw new ELException(e);
            }
        }

        @Override
        public String identity() {
            return ELMethod.identity(registration.declaringType(), registration.name(),
                registration.parameterTypes());
        }

        @Override
        public String toString() {
            return identity();
        }
    }

    /**
     * Dispatches the registered methods, selecting an overload as the section 1.6 of the specification
     * requires.
     */
    private static final class RegisteredMethodExecutor implements ELMethodExecutor {

        private final Map<Class<?>, Map<String, List<Registration>>> declarations;
        private final Map<String, Registration> functions;
        private final int order;

        /**
         * The registrations that apply to a type, its own merged with the ones its supertypes and interfaces
         * declare, resolved once per type.
         */
        private final ClassValue<Map<String, List<ELMethods.Candidate<RegisteredMethod>>>> resolved =
            new ClassValue<>() {
                @Override
                protected Map<String, List<ELMethods.Candidate<RegisteredMethod>>> computeValue(Class<?> type) {
                    Map<String, List<Registration>> inherited = new HashMap<>();
                    collect(type, inherited, true);
                    Map<String, List<ELMethods.Candidate<RegisteredMethod>>> byName = new HashMap<>();
                    inherited.forEach((name, list) -> {
                        boolean reusable = list.size() == 1;
                        List<ELMethods.Candidate<RegisteredMethod>> candidates = new ArrayList<>(list.size());
                        for (Registration registration : list) {
                            candidates.add(new ELMethods.Candidate<>(
                                new RegisteredMethod(registration, reusable),
                                registration.parameterTypes(), registration.varArgs(), registration.isStatic(),
                                false));
                        }
                        byName.put(name, List.copyOf(candidates));
                    });
                    return byName;
                }
            };

        private RegisteredMethodExecutor(Map<Class<?>, Map<String, List<Registration>>> declarations,
                                         Map<String, Registration> functions,
                                         int order) {
            this.declarations = declarations;
            this.functions = functions;
            this.order = order;
        }

        @Override
        public int getOrder() {
            return order;
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
            boolean isStatic = base instanceof ELClass;
            Class<?> type = isStatic ? ((ELClass) base).getKlass() : base.getClass();
            List<ELMethods.Candidate<RegisteredMethod>> candidates = resolved.get(type).get(method.toString());
            if (candidates == null || candidates.isEmpty()) {
                return null;
            }
            // the name is registered, so an argument list that fits none of its overloads is an error of the
            // expression rather than a reason to fall through to the next executor
            return ELMethods.select(type, method.toString(), candidates,
                argumentTypes == null ? null : Argument.toClassArray(argumentTypes),
                arguments == null ? NO_ARGUMENTS : arguments, isStatic);
        }

        @Override
        @Nullable
        public ELMethod resolveFunction(ELContext context, String prefix, String localName) {
            Registration registration = functions.get(qualifiedName(prefix, localName));
            return registration == null ? null : new RegisteredMethod(registration, true);
        }

        private void collect(Class<?> type, Map<String, List<Registration>> into, boolean declaringType) {
            Map<String, List<Registration>> declared = declarations.get(type);
            if (declared != null) {
                declared.forEach((name, list) -> {
                    // a constructor is not inherited: `Child(...)` must not select the constructor registered
                    // for `Parent`, which would construct a `Parent` instead
                    if (declaringType || !CONSTRUCTOR.equals(name)) {
                        into.computeIfAbsent(name, ignored -> new ArrayList<>(list.size())).addAll(list);
                    }
                });
            }
            for (Class<?> anInterface : type.getInterfaces()) {
                collect(anInterface, into, false);
            }
            Class<?> superclass = type.getSuperclass();
            if (superclass != null) {
                collect(superclass, into, false);
            }
        }

        @Override
        public String toString() {
            return "ELMethodRegistry" + Arrays.toString(declarations.keySet().toArray());
        }
    }
}
