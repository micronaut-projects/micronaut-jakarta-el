package example

import io.micronaut.el.annotation.ELFunction
import jakarta.inject.Singleton

@Singleton // <1>
class PricingService {

    private final double discount = 0.1

    @ELFunction(prefix = "pricing") // <2>
    double quote(Book book, int quantity) {
        book.unitPrice * quantity * (1 - discount)
    }

    @ELFunction(prefix = "pricing") // <3>
    static String currency() {
        "EUR"
    }
}
