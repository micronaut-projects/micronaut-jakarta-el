package io.micronaut.el.benchmark;

/**
 * A user-defined functional interface the lambda of the {@code customLambda} benchmark is coerced to.
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
