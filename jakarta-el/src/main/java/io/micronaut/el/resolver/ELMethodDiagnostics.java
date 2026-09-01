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

import io.micronaut.context.BeanDefinitionRegistry;
import io.micronaut.core.annotation.Internal;
import io.micronaut.core.beans.BeanIntrospection;
import io.micronaut.core.beans.BeanIntrospector;
import io.micronaut.core.beans.BeanMethod;
import io.micronaut.core.type.Argument;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.inject.BeanDefinition;
import io.micronaut.inject.ExecutableMethod;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.MethodNotFoundException;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.StringJoiner;

/**
 * Builds the {@link MethodNotFoundException} of a method that no executor and no resolver of the chain could
 * resolve, saying why each of the descriptions a method can be reached through did not answer.
 *
 * <p>A method is dispatched from the bean introspection of its type, from the executable metadata of its bean
 * definition, or reflectively, and each of the three has a precondition an application can fail to meet: the
 * type is not {@code @Introspected}, the method is not {@code @Executable}, the {@link ELContext} carries no
 * bean context for the executable metadata to be read from, or the optional reflection module is not on the
 * classpath. Reporting only the name of the method leaves an author of an expression with no way to tell which
 * of them it was, and the one that is the least visible — a context carrying no bean context, which makes
 * {@link ExecutableMethodELExecutor} decline silently — is the one that reads as a method that plainly exists
 * not being found.</p>
 *
 * <p>Nothing here runs on the happy path. Every check is made once, on the path that is about to throw, and
 * none of it changes what resolves: an executor that declined goes on declining, and this only describes the
 * decline.</p>
 *
 * @author Denis Stepanov
 * @since 1.0.1
 */
@Internal
public final class ELMethodDiagnostics {

    /**
     * The package of the executor of the optional reflection module. It is matched by name because this module
     * does not depend on that one; an executor of that package resolves any public method, so its absence is
     * itself a remedy to report.
     */
    private static final String REFLECTION_PACKAGE = "io.micronaut.el.interpreter.reflection.";

    private static final String REFLECTION_MODULE = "micronaut-jakarta-el-interpreter-reflection";

    private ELMethodDiagnostics() {
    }

    /**
     * The exception of a method the resolver chain of a context did not resolve.
     *
     * <p>The reflection module is not reported as a remedy for this path: the chain is not the executors, and
     * the standard one ends in the reflective resolvers, so a method that reached the end of it was looked up
     * reflectively already.</p>
     *
     * @param context   The context the expression was evaluated with
     * @param base      The base object, or an {@link ELClass} for a static method
     * @param method    The method name
     * @param arguments The evaluated arguments
     * @return The exception to throw
     */
    public static MethodNotFoundException notFound(ELContext context,
                                                   Object base,
                                                   Object method,
                                                   Object @Nullable [] arguments) {
        return new MethodNotFoundException(describe(context, base, method, arguments, Reflection.CHAIN));
    }

    /**
     * The exception of a method none of the executors of an expression parsed at runtime resolved.
     *
     * @param context   The context the expression was evaluated with
     * @param base      The base object, or an {@link ELClass} for a static method
     * @param method    The method name
     * @param arguments The evaluated arguments
     * @param executors The executors that were consulted, in order
     * @return The exception to throw
     */
    public static MethodNotFoundException notFound(ELContext context,
                                                   Object base,
                                                   Object method,
                                                   Object @Nullable [] arguments,
                                                   List<? extends ELMethodExecutor> executors) {
        return new MethodNotFoundException(describe(context, base, method, arguments, reflection(executors)));
    }

    private static String describe(ELContext context,
                                   Object base,
                                   Object method,
                                   Object @Nullable [] arguments,
                                   Reflection reflection) {
        Class<?> type = base instanceof ELClass elClass ? elClass.getKlass() : base.getClass();
        int count = arguments == null ? 0 : arguments.length;
        StringBuilder message = new StringBuilder(160)
            .append("Cannot find the method '").append(method).append("' of ").append(type.getName())
            .append(" accepting ").append(count).append(" argument(s).");
        if (method instanceof String name && !(base instanceof ELClass) && !ExecutableMethodELExecutor.declined(base)) {
            // the bases the specification, or this module, resolves elsewhere are not reached through a bean
            // definition or an introspection, so neither has anything to say about them
            executableMethods(message, context, type, name, count);
            introspection(message, type, name, count);
        }
        message.append(reflection.sentence);
        return message.toString();
    }

    /**
     * How, if at all, the path that failed would have reached the method reflectively, which decides whether
     * the optional module is a remedy to name or the reflective lookup is a cause that has been ruled out.
     */
    private enum Reflection {

        /**
         * No executor of the reflection module was consulted, so the method was never looked up reflectively.
         */
        ABSENT(" No reflective executor is registered either, so the method was not looked up reflectively:"
            + " add the " + REFLECTION_MODULE + " module to resolve any public method."),

        /**
         * The reflective executor of the optional module was one of the executors that declined.
         */
        EXECUTOR(" The reflective executor was consulted as well and found no public method of that name"
            + " accepting the arguments given."),

        /**
         * The resolver chain of the context declined, and the standard chain ends in the reflective resolvers.
         */
        CHAIN(" The resolvers of the chain declined too, and the standard chain looks a method up reflectively,"
            + " so no public method of that name accepts the arguments given either.");

        private final String sentence;

        Reflection(String sentence) {
            this.sentence = sentence;
        }
    }

    /**
     * What the executable metadata of the bean definitions has to say: whether a bean context was registered on
     * the context at all, whether the type is a bean of it, and whether its definition carries the method.
     */
    private static void executableMethods(StringBuilder message,
                                          ELContext context,
                                          Class<?> type,
                                          String name,
                                          int count) {
        BeanDefinitionRegistry registry = ExecutableMethodELExecutor.registryOf(context);
        if (registry == null) {
            message.append(" No bean context is registered in this ELContext, so the executable methods of the")
                .append(" bean definitions were not consulted: register one with")
                .append(" context.putContext(BeanDefinitionRegistry.class, beanContext), or evaluate the")
                .append(" expression with new CompiledELContext(beanContext).");
            return;
        }
        BeanDefinition<Object> definition = ExecutableMethodELExecutor.definitionOf(registry, type);
        if (definition == null) {
            message.append(" The bean context registered in this ELContext has no bean definition for ")
                .append(type.getName())
                .append(", so it carries no executable method: only a bean of that context is reached that way.");
            return;
        }
        List<String> overloads = new ArrayList<>(1);
        for (ExecutableMethod<Object, ?> executable : definition.getExecutableMethods()) {
            if (executable.getMethodName().equals(name)) {
                overloads.add(signature(name, executable.getArguments()));
            }
        }
        if (overloads.isEmpty()) {
            message.append(' ').append(type.getName())
                .append(" is a bean of the bean context registered in this ELContext, but its definition carries")
                .append(" no executable method named '").append(name)
                .append("': annotate the method with @Executable, directly or through an annotation")
                .append(" meta-annotated with it, so that it is compiled into the bean definition.");
        } else {
            message.append(" The bean definition of ").append(type.getName()).append(" carries ")
                .append(String.join(", ", overloads)).append(", but the ").append(count)
                .append(" argument(s) given select none of them.");
        }
    }

    /**
     * What the bean introspection of the type has to say: whether the type carries one, and whether the method
     * entered it.
     */
    private static void introspection(StringBuilder message, Class<?> type, String name, int count) {
        BeanIntrospection<?> introspection = BeanIntrospector.SHARED.findIntrospection(type).orElse(null);
        if (introspection == null) {
            message.append(" The type carries no bean introspection either: annotate it with @Introspected, and")
                .append(" the method with @Executable, to have the method dispatched from generated metadata.");
            return;
        }
        List<String> overloads = new ArrayList<>(1);
        for (BeanMethod<?, ?> beanMethod : introspection.getBeanMethods()) {
            if (beanMethod.getName().equals(name)) {
                overloads.add(signature(name, beanMethod.getArguments()));
            }
        }
        if (overloads.isEmpty()) {
            message.append(" Its bean introspection carries no method named '").append(name)
                .append("': annotate the method with @Executable so that it enters the introspection.");
        } else {
            message.append(" Its bean introspection carries ").append(String.join(", ", overloads))
                .append(", but the ").append(count).append(" argument(s) given select none of them.");
        }
    }

    private static String signature(String name, Argument<?>[] arguments) {
        StringJoiner signature = new StringJoiner(", ", name + "(", ")");
        for (Argument<?> argument : arguments) {
            signature.add(argument.getType().getName());
        }
        return signature.toString();
    }

    private static Reflection reflection(List<? extends ELMethodExecutor> executors) {
        for (ELMethodExecutor executor : executors) {
            if (executor.getClass().getName().startsWith(REFLECTION_PACKAGE)) {
                return Reflection.EXECUTOR;
            }
        }
        return Reflection.ABSENT;
    }
}
