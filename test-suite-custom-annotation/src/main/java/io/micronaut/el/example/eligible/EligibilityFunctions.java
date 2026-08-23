package io.micronaut.el.example.eligible;

import io.micronaut.el.annotation.ELFunction;

/**
 * The functions available to the conditions of {@link Eligible}, under the {@code fn} prefix.
 */
public final class EligibilityFunctions {

    private EligibilityFunctions() {
    }

    @ELFunction
    public static boolean adult(int age) {
        return age >= 18;
    }

    @ELFunction("inEurope")
    public static boolean isEuropean(String country) {
        return country.equals("CZ") || country.equals("DE") || country.equals("FR");
    }
}
