package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Size;

/** The same messages as {@link Order}, with the {@code ${...}} delimiters of the Jakarta Validation specification. */
@Introspected
public class OrderDollar {

    @Size(min = 5, message = "value ${validatedValue.toUpperCase()} must be at least {min}")
    private String code;

    @Min(value = 100, message = "${formatter.format('%1$.2f', validatedValue)} must be larger than {value}")
    private double amount;

    public OrderDollar(String code, double amount) {
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
