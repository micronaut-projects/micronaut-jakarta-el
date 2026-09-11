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
import org.jspecify.annotations.Nullable;
import jakarta.el.ELContext;
import jakarta.el.ELResolver;

import java.io.File;
import java.io.FileDescriptor;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.List;
import java.util.ServiceLoader;

/**
 * What an expression parsed at runtime may reach through reflection.
 *
 * <p>An expression declared with {@code io.micronaut.el.annotation.ELExpression} is written by the developer
 * and compiled, so it is as trusted as the rest of the source. An expression string built at runtime is not:
 * the specification resolves properties, methods, static members and constructors dynamically, and where that
 * resolution reflects, an expression that reaches {@code java.lang.Runtime}, a {@code java.lang.Class} or the
 * reflection API can run whatever the process can. A sandbox closes those paths while leaving the language
 * intact.</p>
 *
 * <p>The sandbox is consulted where the resolution of such an expression reflects, and nowhere else: a method,
 * a constructor, a static member or a function an {@link ELMethodExecutor#isReflective() executor resolves
 * reflectively}, and a property a resolver reads that is not known to read it without reflection - the resolvers
 * of the specification that read a bean (an {@code Optional} holding one included), a record, a class or a static
 * import, and any resolver the module does not know. It is asked about the base object before the access and about
 * the value the access produced after it, so reflection neither works on nor hands the expression a type it denies.
 * What the application described while it compiled - its bean introspections, the executable methods of its beans,
 * the methods it registered - and the maps, lists and arrays an expression indexes are reached without it: they
 * lead only where the application already chose to lead. An expression compiled at compilation time never
 * consults it.</p>
 *
 * <p>{@link #standard()} is applied to every expression the
 * {@code micronaut-jakarta-el-interpreter} module creates. Register another one, {@link #UNRESTRICTED}
 * included, on the context the expression is evaluated with:</p>
 *
 * <pre>{@code context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);}</pre>
 *
 * <p>A sandbox is not a security boundary on its own: it bounds what reflection reaches, not what the beans
 * an expression reaches then do. Treat an expression string from an untrusted source as untrusted input.</p>
 *
 * @author Denis Stepanov
 * @since 1.1
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
    static ELSandbox of(@Nullable ELContext context) {
        Object registered = context == null ? null : context.getContext(ELSandbox.class);
        return registered instanceof ELSandbox sandbox ? sandbox : standard();
    }

    /**
     * Whether reflection may reach the members of a type, which is asked of the base object of every reflective
     * access and of the value every reflective access produces.
     *
     * <p>No member is denied by its name: every member that leads from an allowed object to a denied one, such as
     * {@code getClass} or {@code getClassLoader}, produces a value of a denied type, and is stopped by that.</p>
     *
     * @param type The type the expression reached
     * @return Whether the expression may use it
     */
    boolean allowsType(Class<?> type);

    /**
     * The default deny list.
     *
     * <p>Types are denied together with their subtypes, and by package: a subclass of a denied type is denied,
     * so a custom class loader does not slip through the check on {@link ClassLoader}.</p>
     */
    final class StandardELSandbox implements ELSandbox {

        static final StandardELSandbox INSTANCE = new StandardELSandbox();

        /**
         * The types that hand an expression the process, the class loaders or the reflection API. Every
         * subtype of one of them is denied too, which {@link Class#isAssignableFrom} answers from the class
         * itself, without asking it for its interfaces.
         */
        static final List<Class<?>> DENIED_TYPES = List.of(
            Class.class,
            ClassLoader.class,
            Module.class,
            ModuleLayer.class,
            Package.class,
            Process.class,
            ProcessBuilder.class,
            ProcessHandle.class,
            Runtime.class,
            System.class,
            Thread.class,
            ThreadGroup.class,
            File.class,
            FileDescriptor.class,
            URI.class,
            URL.class,
            Path.class,
            ServiceLoader.class,
            ELContext.class,
            ELResolver.class
        );

        /**
         * The packages whose every type is denied. Only the packages the platform owns are listed:
         * {@code com.sun} is not one of them, applications and specification kits alike publish under it,
         * and what it holds that matters is reached through a package that is listed or through reflection.
         */
        static final List<String> DENIED_PACKAGES = List.of(
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
         * The verdict per class, computed once: the comparison with every denied type is not worth repeating
         * for every access an expression makes.
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
            for (Class<?> denied : DENIED_TYPES) {
                if (denied.isAssignableFrom(component)) {
                    return false;
                }
            }
            return true;
        }
    }
}
