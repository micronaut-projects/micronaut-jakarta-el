package io.micronaut.el.test

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected
class Book {

    String title
    final String category
    double unitPrice

    Book(String title, String category, double unitPrice) {
        this.title = title
        this.category = category
        this.unitPrice = unitPrice
    }

    @Executable
    double discounted(double percent) {
        unitPrice - (unitPrice * percent / 100)
    }
}
