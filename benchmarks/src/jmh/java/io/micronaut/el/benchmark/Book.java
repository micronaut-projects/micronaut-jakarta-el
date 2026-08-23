package io.micronaut.el.benchmark;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.util.List;
import java.util.Map;

/**
 * The bean the benchmarked expressions refer to as {@code book}.
 */
@Introspected
public class Book {

    private final String title;
    private final double unitPrice;
    private final int pages;
    private final Author author;
    private final List<String> tags;
    private final Map<String, String> attributes;

    public Book(String title, double unitPrice, int pages, Author author, List<String> tags, Map<String, String> attributes) {
        this.title = title;
        this.unitPrice = unitPrice;
        this.pages = pages;
        this.author = author;
        this.tags = tags;
        this.attributes = attributes;
    }

    public String getTitle() {
        return title;
    }

    public double getUnitPrice() {
        return unitPrice;
    }

    public int getPages() {
        return pages;
    }

    public Author getAuthor() {
        return author;
    }

    public List<String> getTags() {
        return tags;
    }

    public Map<String, String> getAttributes() {
        return attributes;
    }

    @Executable
    public double discounted(double percent) {
        return unitPrice * (1 - percent / 100);
    }

    @Executable
    public double adjusted(PriceAdjuster adjuster) {
        return adjuster.adjust(unitPrice, pages);
    }
}
