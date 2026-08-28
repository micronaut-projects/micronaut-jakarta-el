package io.micronaut.el.processor;

import io.micronaut.annotation.processing.test.JavaParser;
import io.micronaut.el.ELExpressionSource;

import javax.tools.JavaFileObject;
import java.io.InputStream;
import java.util.HashMap;
import java.util.Map;

/**
 * Compiles a source with the annotation processor and loads what it generated, so that a test asserts on what
 * the generated classes do rather than on what they look like.
 *
 * @param loader   The loader the generated classes and the classes of the source were defined by
 * @param registry The registry generated for the declared expressions
 */
record GeneratedExpressions(ClassLoader loader, ELExpressionSource registry) {

    /**
     * @param className The class declaring the expressions
     * @param source    Its source
     * @return The registry generated for it
     */
    static GeneratedExpressions of(String className, String source) throws Exception {
        Map<String, byte[]> classes = new HashMap<>();
        try (JavaParser parser = new JavaParser()) {
            for (JavaFileObject file : parser.generate(className, source)) {
                if (!file.getName().endsWith(".class")) {
                    continue;
                }
                try (InputStream input = file.openInputStream()) {
                    classes.put(binaryName(file.getName(), className), input.readAllBytes());
                }
            }
        }
        ClassLoader loader = new ClassLoader(GeneratedExpressions.class.getClassLoader()) {
            @Override
            protected Class<?> findClass(String name) throws ClassNotFoundException {
                byte[] bytes = classes.get(name);
                if (bytes == null) {
                    throw new ClassNotFoundException(name);
                }
                return defineClass(name, bytes, 0, bytes.length);
            }
        };
        return new GeneratedExpressions(loader,
            (ELExpressionSource) loader.loadClass(className + "$ELExpressions")
                .getDeclaredConstructor()
                .newInstance());
    }

    /**
     * @param name The name of the loaded class
     * @return A new instance of it
     */
    Object instantiate(String name) throws Exception {
        return loader.loadClass(name).getDeclaredConstructor().newInstance();
    }

    private static String binaryName(String fileName, String declaringClass) {
        String path = fileName.replace('\\', '/');
        String packagePath = declaringClass.substring(0, declaringClass.lastIndexOf('.')).replace('.', '/');
        int start = path.indexOf(packagePath);
        return path.substring(start, path.length() - ".class".length()).replace('/', '.');
    }
}
