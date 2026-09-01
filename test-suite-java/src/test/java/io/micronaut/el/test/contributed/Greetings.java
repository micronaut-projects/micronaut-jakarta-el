package io.micronaut.el.test.contributed;

/**
 * The compile-time counterpart of the function contributed as {@code greet:twice}.
 */
public final class Greetings {

    private Greetings() {
    }

    public static String twice(String whom) {
        return whom + whom;
    }
}
