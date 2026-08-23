package io.micronaut.el.example.eligible;

import io.micronaut.el.annotation.ELFunction;

/**
 * The functions available to the conditions of {@link Eligible}, under the {@code fn} prefix, which the
 * processor of the annotation lists for the expressions of the modules depending on this one.
 */
// tag::functions[]
public final class EligibilityFunctions {

    private EligibilityFunctions() {
    }

    @ELFunction(prefix = "fn") // <1>
    public static boolean adult(int age) {
        return age >= 18;
    }

    @ELFunction(prefix = "fn", name = "inEurope") // <2>
    public static boolean isEuropean(String country) {
        return country.equals("CZ") || country.equals("DE") || country.equals("FR");
    }
}
// end::functions[]
