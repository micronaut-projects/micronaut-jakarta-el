package io.micronaut.el.test.parity;

/**
 * The generated twin of the {@code Greeter} of {@code ELMethodContributorTest}, with the single
 * {@code greet(String)} that the contributor registers in most of its scenarios, and the {@code pick} that its
 * supertype scenario registers on the supertype.
 */
public class ParityGreeter {

    private final String name;

    public ParityGreeter(String name) {
        this.name = name;
    }

    public String getName() {
        return name;
    }

    public String greet(String whom) {
        return "hello " + whom + ", " + name;
    }

    public String pick(Number value) {
        return "parent";
    }
}
