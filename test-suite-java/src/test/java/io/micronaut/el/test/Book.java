package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

@Introspected
public class Book {

    private String title;
    private String category;
    private double unitPrice;

    public Book(String title, String category, double unitPrice) {
        this.title = title;
        this.category = category;
        this.unitPrice = unitPrice;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getCategory() {
        return category;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public void setUnitPrice(double unitPrice) {
        this.unitPrice = unitPrice;
    }

    public String describe() {
        return title + " (" + category + ")";
    }

    @Executable
    public double discounted(double percent) {
        return unitPrice * (1 - percent / 100);
    }
}
