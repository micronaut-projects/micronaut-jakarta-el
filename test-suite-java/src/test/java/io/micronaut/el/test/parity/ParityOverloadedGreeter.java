package io.micronaut.el.test.parity;

/**
 * The generated twin of the {@code Greeter} of {@code ELMethodContributorTest} in its overloaded scenario, where
 * both {@code greet(String)} and {@code greet(Integer)} are registered.
 */
public final class ParityOverloadedGreeter {

    private final String name;

    public ParityOverloadedGreeter(String name) {
        this.name = name;
    }

    public String greet(String whom) {
        return "hello " + whom + ", " + name;
    }

    public String greet(Integer times) {
        return "hello ".repeat(times);
    }
}
