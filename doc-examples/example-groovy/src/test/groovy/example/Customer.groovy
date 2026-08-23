package example

import io.micronaut.core.annotation.Introspected

@Introspected
class Customer {
    final String name
    final int age
    final String country

    Customer(String name, int age, String country) {
        this.name = name
        this.age = age
        this.country = country
    }
}
