package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/**
 * The message patterns of the Jakarta Validation TCK, written with {@code #{...}} so that Micronaut's property
 * placeholders and evaluated expressions both stay out of them.
 */
@Introspected
public class Order {

    @Size(min = 5, message = "value #{validatedValue.toUpperCase()} must be at least {min}")
    private String code;

    @Min(value = 100, message = "#{formatter.format('%1$.2f', validatedValue)} must be larger than {value}")
    private double amount;

    public Order(String code, double amount) {
        this.code = code;
        this.amount = amount;
    }

    public String getCode() {
        return code;
    }

    public double getAmount() {
        return amount;
    }
}
