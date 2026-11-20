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

import java.io.Console;
import java.io.File;
import java.io.FileDescriptor;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.RandomAccessFile;
import java.io.Reader;
import java.io.Writer;
import java.net.URI;
import java.net.URL;
import java.nio.file.Path;
import java.util.Formatter;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.TimeZone;
import java.util.zip.ZipFile;

/**
 * What an expression parsed at runtime may reach through reflection.
 *
 * <p>An expression declared with {@code io.micronaut.el.annotation.ELExpression} is written by the developer
 * and compiled, so it is as trusted as the rest of the source. An expression string built at runtime is not:
 * the specification resolves properties, methods, static members and constructors dynamically, and where that
 * resolution reflects, an expression that reaches {@code java.lang.Runtime}, a {@code java.lang.Class} or the
 * reflection API can run whatever the process can. A sandbox closes the paths it knows of while leaving the
 * language intact.</p>
 *
 * <p>The sandbox is consulted where the resolution of such an expression reflects, and nowhere else: a method,
 * a constructor, a static member or a function an {@link ELMethodExecutor#isReflective() executor resolves
 * reflectively}, a class an identifier names through the imports, and a property a resolver reads that is not
 * known to read it without reflection - the resolvers of the specification that read a bean (an {@code Optional}
 * holding one included), a record, a class or a static import, and any resolver the module does not know. Before
 * the access it is asked about the base object - for a constructor or a static method, the class they belong to -
 * and about the name of a method; after it, about the value the access produced. Reflection therefore neither
 * works on nor hands the expression a type it denies. What the application described while it compiled - its bean
 * introspections, the executable methods of its beans, the methods it registered - and the maps, lists and arrays
 * an expression indexes are reached without it: they lead only where the application already chose to lead. An
 * expression compiled at compilation time never consults it.</p>
 *
 * <p>{@link #standard()} is applied to every expression the
 * {@code micronaut-jakarta-el-interpreter} module creates. Register another one, {@link #UNRESTRICTED}
 * included, on the context the expression is evaluated with:</p>
 *
 * <pre>{@code context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);}</pre>
 *
 * <p><b>A sandbox is not a security boundary.</b> The standard one is a deny list of the types and members of
 * the platform known to reach the process, the file system, the network, the class loaders, reflection or the
 * execution of code. It cannot know what an application class, a library of the classpath, or a type it does not
 * list does once an expression reaches it, and it does not bound the time or the memory an expression consumes.
 * Do not evaluate an expression string from an untrusted source; an application that has to should evaluate it
 * with a context that exposes nothing it is not prepared to have called, and treat the sandbox as one layer of
 * defence among others.</p>
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
        public boolean allowsMethod(Class<?> type, String name) {
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
     * access, of the class an imported identifier names, and of the value every reflective access produces.
     *
     * <p>Every member that leads from an allowed object to a denied one, such as {@code getClass} or
     * {@code getClassLoader}, produces a value of a denied type, and is stopped by that.</p>
     *
     * @param type The type the expression reached
     * @return Whether the expression may use it
     */
    boolean allowsType(Class<?> type);

    /**
     * Whether reflection may invoke the method of the given name on an instance of a type, or as a static method
     * or a constructor ({@code <init>}) of it, which is asked before the method runs. A method whose effect is
     * done by the time its value could be checked, such as one that reads a system property and returns it as a
     * plain number, is denied here.
     *
     * <p>The default implementation allows the method when the type is allowed.</p>
     *
     * @param type The class of the base object, or the class declaring a static method or a constructor
     * @param name The name of the method
     * @return Whether the expression may invoke it
     */
    default boolean allowsMethod(Class<?> type, String name) {
        return allowsType(type);
    }

    /**
     * The default deny list.
     *
     * <p>Types are denied together with their subtypes, and by package: a subclass of a denied type is denied,
     * so a custom class loader does not slip through the check on {@link ClassLoader}. A class of the platform
     * that its module does not export, such as the {@code sun.util.calendar.ZoneInfo} a {@link TimeZone} is, is
     * judged by the nearest superclass its module exports, since that is all of it reflection can reach.</p>
     */
    final class StandardELSandbox implements ELSandbox {

        static final StandardELSandbox INSTANCE = new StandardELSandbox();

        /**
         * The types that hand an expression the process, the class loaders, the reflection API, the file system
         * or a stream of bytes. Every subtype of one of them is denied too, which {@link Class#isAssignableFrom}
         * answers from the class itself, without asking it for its interfaces.
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
            StackWalker.class,
            securityManager(),
            File.class,
            FileDescriptor.class,
            RandomAccessFile.class,
            Console.class,
            InputStream.class,
            OutputStream.class,
            Reader.class,
            Writer.class,
            URI.class,
            URL.class,
            Path.class,
            // a Formatter opens the file it is given the name of, and a ZipFile, a JarFile included, reads it
            Formatter.class,
            ZipFile.class,
            ServiceLoader.class,
            ELContext.class,
            ELResolver.class
        );

        /**
         * The packages whose every type is denied: the files and streams, the reflection and invocation API, the
         * modules and the instrumentation, the network, the files and channels of NIO, the beans decoder, the
         * databases, the management and naming services, the remote invocation, the scripting engines and the
         * compiler, the logging and the preferences that write files, and the XML processors that load documents
         * and extensions.
         *
         * <p>{@code jdk.} and {@code sun.} are denied as well: the packages of those names a module exports are
         * tools and unsupported API. The ones it does not export are only reached as the public type they
         * extend, see {@link StandardELSandbox}. {@code com.sun} is not listed, applications and specification
         * kits alike publish under it; the packages of that name the JDK ships are denied with the {@code jdk.}
         * modules that hold them.</p>
         */
        static final List<String> DENIED_PACKAGES = List.of(
            "java.beans.",
            "java.io.",
            "java.lang.instrument.",
            "java.lang.invoke.",
            "java.lang.management.",
            "java.lang.module.",
            "java.lang.reflect.",
            "java.net.",
            "java.nio.channels.",
            "java.nio.file.",
            "java.rmi.",
            "java.security.",
            "java.sql.",
            "java.util.jar.",
            "java.util.logging.",
            "java.util.prefs.",
            "javax.annotation.processing.",
            "javax.lang.model.",
            "javax.management.",
            "javax.naming.",
            "javax.net.",
            "javax.rmi.",
            "javax.script.",
            "javax.sql.",
            "javax.tools.",
            "javax.xml.catalog.",
            "javax.xml.parsers.",
            "javax.xml.stream.",
            "javax.xml.transform.",
            "javax.xml.validation.",
            "javax.xml.xpath.",
            "org.xml.sax.",
            "jdk.",
            "sun."
        );

        /**
         * The methods of allowed classes that act before their value could be checked: the static methods that
         * read a system property and return it as a plain boolean or number, the one that loads a resource bundle,
         * which instantiates a class by its name, and the ones that change the defaults of the whole process.
         */
        static final Map<Class<?>, Set<String>> DENIED_METHODS = Map.of(
            Boolean.class, Set.of("getBoolean"),
            Integer.class, Set.of("getInteger"),
            Long.class, Set.of("getLong"),
            ResourceBundle.class, Set.of("getBundle", "clearCache"),
            Locale.class, Set.of("setDefault"),
            TimeZone.class, Set.of("setDefault")
        );

        /**
         * The packages of the platform whose classes are never judged by their public superclass: what they hold
         * is only ever reached on purpose.
         */
        private static final List<String> ALWAYS_DENIED_PACKAGES = List.of(
            "jdk.internal.",
            "sun.misc.",
            "sun.reflect.",
            "java.lang.invoke.",
            "java.lang.reflect."
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
        public boolean allowsMethod(Class<?> type, String name) {
            if (!allowsType(type)) {
                return false;
            }
            for (Map.Entry<Class<?>, Set<String>> denied : DENIED_METHODS.entrySet()) {
                if (denied.getValue().contains(name) && denied.getKey().isAssignableFrom(type)) {
                    return false;
                }
            }
            return true;
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
            for (Class<?> denied : DENIED_TYPES) {
                if (denied.isAssignableFrom(component)) {
                    return false;
                }
            }
            String name = component.getName();
            if (startsWithAny(name, ALWAYS_DENIED_PACKAGES)) {
                return false;
            }
            Class<?> api = publicType(component);
            if (api != component) {
                // reflection reaches the class as its exported superclass, or as an Object
                return api == Object.class || ALLOWED.get(api);
            }
            if (startsWithAny(name, DENIED_PACKAGES)) {
                // an exception of java.io is a value an expression may read the message of
                return name.startsWith("java.io.") && Throwable.class.isAssignableFrom(component);
            }
            Module module = component.getModule();
            // the com.sun packages and the tools the jdk. modules of the platform export
            return !(module.isNamed() && module.getLayer() == ModuleLayer.boot()
                && module.getName().startsWith("jdk."));
        }

        /**
         * The type reflection reaches a class as: the class itself when its module exports its package, which a
         * class of the classpath always is, and otherwise its nearest superclass that is exported, since the
         * members the class does not inherit from an exported type cannot be invoked from outside its module.
         */
        private static Class<?> publicType(Class<?> type) {
            Class<?> current = type;
            while (current != null && current != Object.class) {
                Module module = current.getModule();
                if (!module.isNamed() || module.isExported(current.getPackageName())) {
                    return current;
                }
                current = current.getSuperclass();
            }
            return Object.class;
        }

        private static boolean startsWithAny(String name, List<String> prefixes) {
            for (String prefix : prefixes) {
                if (name.startsWith(prefix)) {
                    return true;
                }
            }
            return false;
        }

        // the type is deprecated for removal, and still one an expression must not reach while it exists
        @SuppressWarnings("removal")
        private static Class<?> securityManager() {
            return SecurityManager.class;
        }
    }
}
