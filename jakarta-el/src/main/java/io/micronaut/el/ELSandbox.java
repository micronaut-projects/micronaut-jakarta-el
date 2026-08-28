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

import io.micronaut.core.annotation.Experimental;
import jakarta.el.ELContext;

import java.util.List;
import java.util.Set;

/**
 * The types and members an expression parsed at runtime may reach.
 *
 * <p>An expression declared with {@code io.micronaut.el.annotation.ELExpression} is written by the developer
 * and compiled, so it is as trusted as the rest of the source. An expression string built at runtime is not:
 * the specification resolves properties, methods, static members and constructors dynamically, so an
 * expression that reaches {@code java.lang.Runtime}, a {@code java.lang.Class} or the reflection API can run
 * whatever the process can. A sandbox closes those paths while leaving the language intact.</p>
 *
 * <p>{@link #standard()} is applied to every expression the
 * {@code micronaut-jakarta-el-interpreter} module creates. Register another one, {@link #UNRESTRICTED}
 * included, on the context the expression is evaluated with:</p>
 *
 * <pre>{@code context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);}</pre>
 *
 * <p>A sandbox is not a security boundary on its own: it bounds what an expression reaches, not what the
 * beans it reaches then do. Treat an expression string from an untrusted source as untrusted input.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 * @see ELSandboxException
 */
@Experimental
public interface ELSandbox {

    /**
     * The sandbox allowing everything, which is how an expression compiled at compilation time is evaluated.
     */
    ELSandbox UNRESTRICTED = new ELSandbox() {

        @Override
        public boolean allowsType(Class<?> type) {
            return true;
        }

        @Override
        public boolean allowsMember(Class<?> type, String member) {
            return true;
        }

        @Override
        public String toString() {
            return "ELSandbox.UNRESTRICTED";
        }
    };

    /**
     * The sandbox applied to the expressions parsed at runtime: it denies the types through which an
     * expression escapes into arbitrary Java, and lets everything else through.
     *
     * @return The standard sandbox
     */
    static ELSandbox standard() {
        return StandardELSandbox.INSTANCE;
    }

    /**
     * Reads the sandbox an expression is evaluated under from the context, which is
     * {@link #standard()} unless one was registered with
     * {@code ELContext.putContext(ELSandbox.class, sandbox)}.
     *
     * @param context The context, can be {@code null}
     * @return The sandbox
     */
    static ELSandbox of(ELContext context) {
        Object registered = context == null ? null : context.getContext(ELSandbox.class);
        return registered instanceof ELSandbox sandbox ? sandbox : standard();
    }

    /**
     * Whether an expression may read the properties of a type and invoke its methods, which is asked of the
     * base object of every property access and method invocation, of the class of every static reference and
     * of the class of every constructor reference.
     *
     * @param type The type the expression reached
     * @return Whether the expression may use it
     */
    boolean allowsType(Class<?> type);

    /**
     * Whether an expression may read a property of a type or invoke a method of it, asked once the type
     * itself is allowed.
     *
     * @param type   The type of the base object
     * @param member The name of the property or of the method
     * @return Whether the expression may use it
     */
    boolean allowsMember(Class<?> type, String member);

    /**
     * The default deny list.
     *
     * <p>Types are denied by their own name, by the name of one of their supertypes, and by package: a
     * subclass of a denied type is denied, so a custom class loader does not slip through the check on
     * {@link ClassLoader}.</p>
     */
    final class StandardELSandbox implements ELSandbox {

        static final StandardELSandbox INSTANCE = new StandardELSandbox();

        /**
         * The types that hand an expression the process, the class loaders or the reflection API. Every
         * subtype of one of them is denied too.
         */
        private static final Set<String> DENIED_TYPES = Set.of(
            "java.lang.Class",
            "java.lang.ClassLoader",
            "java.lang.Module",
            "java.lang.ModuleLayer",
            "java.lang.Package",
            "java.lang.Process",
            "java.lang.ProcessBuilder",
            "java.lang.ProcessHandle",
            "java.lang.Runtime",
            "java.lang.System",
            "java.lang.Thread",
            "java.lang.ThreadGroup",
            "java.io.File",
            "java.io.FileDescriptor",
            "java.net.URI",
            "java.net.URL",
            "java.nio.file.Path",
            "java.util.ServiceLoader",
            "jakarta.el.ELContext",
            "jakarta.el.ELResolver"
        );

        /**
         * The packages whose every type is denied. Only the packages the platform owns are listed:
         * {@code com.sun} is not one of them, applications and specification kits alike publish under it,
         * and what it holds that matters is reached through a package that is listed or through reflection.
         */
        private static final List<String> DENIED_PACKAGES = List.of(
            "java.lang.invoke.",
            "java.lang.module.",
            "java.lang.reflect.",
            "java.rmi.",
            "java.security.",
            "javax.naming.",
            "javax.script.",
            "jdk.",
            "sun."
        );

        /**
         * The members that hand an expression a denied type from a type that is allowed. The type of the
         * value they return is denied too, so these only make the failure name what the expression did.
         */
        private static final Set<String> DENIED_MEMBERS = Set.of(
            "class",
            "getClass",
            "classLoader",
            "getClassLoader",
            "module",
            "getModule",
            "protectionDomain",
            "getProtectionDomain",
            "wait",
            "notify",
            "notifyAll"
        );

        /**
         * The verdict per class, computed once: the walk of the supertypes is not worth repeating for every
         * property of every object an expression reaches.
         */
        private static final ClassValue<Boolean> ALLOWED = new ClassValue<>() {
            @Override
            protected Boolean computeValue(Class<?> type) {
                return isAllowed(type);
            }
        };

        private StandardELSandbox() {
        }

        @Override
        public boolean allowsType(Class<?> type) {
            return ALLOWED.get(type);
        }

        @Override
        public boolean allowsMember(Class<?> type, String member) {
            return !DENIED_MEMBERS.contains(member);
        }

        @Override
        public String toString() {
            return "ELSandbox.standard()";
        }

        private static boolean isAllowed(Class<?> type) {
            Class<?> component = type;
            while (component.isArray()) {
                component = component.getComponentType();
            }
            if (component.isPrimitive()) {
                return true;
            }
            String name = component.getName();
            for (String denied : DENIED_PACKAGES) {
                if (name.startsWith(denied)) {
                    return false;
                }
            }
            for (Class<?> supertype = component; supertype != null; supertype = supertype.getSuperclass()) {
                if (DENIED_TYPES.contains(supertype.getName())) {
                    return false;
                }
            }
            return !implementsDenied(component);
        }

        private static boolean implementsDenied(Class<?> type) {
            for (Class<?> anInterface : type.getInterfaces()) {
                if (DENIED_TYPES.contains(anInterface.getName()) || implementsDenied(anInterface)) {
                    return true;
                }
            }
            Class<?> superclass = type.getSuperclass();
            return superclass != null && implementsDenied(superclass);
        }
    }
}
