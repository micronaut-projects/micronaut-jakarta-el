package io.micronaut.el.test.eligible;

import io.micronaut.core.annotation.Introspected;

/** A customer: introspected, so that the condition reads its properties without reflection. */
@Introspected
public record Customer(String name, int age, String country) {
}
