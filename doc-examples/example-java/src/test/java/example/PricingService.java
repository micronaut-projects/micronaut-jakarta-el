package example;

import io.micronaut.el.annotation.ELFunction;
import jakarta.inject.Singleton;

@Singleton // <1>
public class PricingService {

    private final double discount = 0.1;

    @ELFunction(prefix = "pricing") // <2>
    public double quote(Book book, int quantity) {
        return book.getUnitPrice() * quantity * (1 - discount);
    }

    @ELFunction(prefix = "pricing") // <3>
    public static String currency() {
        return "EUR";
    }
}
