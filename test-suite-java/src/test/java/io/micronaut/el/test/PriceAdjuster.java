package io.micronaut.el.test;

/**
 * A user-defined functional interface a lambda expression is coerced to, section 1.25.8 of the specification.
 */
@FunctionalInterface
public interface PriceAdjuster {

    /**
     * @param price    The unit price
     * @param quantity The quantity
     * @return The adjusted price
     */
    double adjust(double price, int quantity);
}
