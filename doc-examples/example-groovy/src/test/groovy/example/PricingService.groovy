package example

import jakarta.inject.Singleton

@Singleton // <1>
class PricingService {

    private final double discount = 0.1

    double quote(Book book, int quantity) { // <2>
        book.unitPrice * quantity * (1 - discount)
    }

    static String currency() { // <3>
        "EUR"
    }
}
