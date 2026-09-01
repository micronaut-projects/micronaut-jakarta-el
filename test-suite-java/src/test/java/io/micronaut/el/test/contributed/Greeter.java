package io.micronaut.el.test.contributed;

/**
 * The compile-time counterpart of {@code io.micronaut.el.interpreter.Greeter}: a plain type carrying no bean
 * introspection, which the interpreter can only reach through a contributed executor and the compiler reaches
 * directly.
 */
public final class Greeter {

    private final String name;

    public Greeter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String greet(String whom) {
        return "hello " + whom + ", " + name;
    }

    public String join(String separator, String... parts) {
        return String.join(separator, parts);
    }
}
