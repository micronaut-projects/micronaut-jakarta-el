package io.micronaut.el.benchmark;

import io.micronaut.core.annotation.Introspected;

/**
 * The author of a {@link Book}, a nested bean.
 */
@Introspected
public class Author {

    private final String name;
    private final int born;

    public Author(String name, int born) {
        this.name = name;
        this.born = born;
    }

    public String getName() {
        return name;
    }

    public int getBorn() {
        return born;
    }
}
