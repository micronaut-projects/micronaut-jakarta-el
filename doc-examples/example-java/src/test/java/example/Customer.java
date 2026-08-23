package example;

import io.micronaut.core.annotation.Introspected;

@Introspected
public record Customer(String name, int age, String country) {
}
