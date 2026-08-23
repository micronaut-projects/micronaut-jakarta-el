package example;

import jakarta.inject.Singleton;

@Singleton // <1>
public class PricingService {

    private final double discount = 0.1;

    public double quote(Book book, int quantity) { // <2>
        return book.getUnitPrice() * quantity * (1 - discount);
    }

    public static String currency() { // <3>
        return "EUR";
    }
}
