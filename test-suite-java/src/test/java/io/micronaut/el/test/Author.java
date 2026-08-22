package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;

import java.util.List;

@Introspected
public class Author {

    private final String name;
    private final List<Book> books;

    public Author(String name, List<Book> books) {
        this.name = name;
        this.books = books;
    }

    public String getName() {
        return name;
    }

    public List<Book> getBooks() {
        return books;
    }

    public String greet(String greeting) {
        return greeting + ", " + name;
    }

    public String greet() {
        return greet("Hello");
    }
}
