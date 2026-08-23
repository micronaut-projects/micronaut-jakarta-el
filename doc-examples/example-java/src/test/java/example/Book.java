package example;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

@Introspected // <1>
public class Book {

    private final String title;
    private final String category;
    private final double unitPrice;

    public Book(String title, String category, double unitPrice) {
        this.title = title;
        this.category = category;
        this.unitPrice = unitPrice;
    }

    public String getTitle() { // <2>
        return title;
    }

    public String getCategory() {
        return category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    @Executable // <3>
    public double discounted(double percent) {
        return unitPrice * (100 - percent) / 100;
    }
}
