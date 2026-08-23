package io.micronaut.el.benchmark;

import io.micronaut.core.annotation.Introspected;

/**
 * A bean the expressions do not declare with {@code @ELVariable}: its properties are resolved at runtime,
 * through the bean introspection on the Micronaut stacks and through {@code BeanELResolver} on the others.
 */
@Introspected
public class Order {

    private final Author customer;

    public Order(Author customer) {
        this.customer = customer;
    }

    public Author getCustomer() {
        return customer;
    }
}
