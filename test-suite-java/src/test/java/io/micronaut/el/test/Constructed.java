package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;

/**
 * Two constructors of which one is strictly more specific, which both execution modes must agree on.
 */
@Introspected
public class Constructed {

    private final String selected;

    public Constructed(Number value) {
        this.selected = "number";
    }

    public Constructed(Object value) {
        this.selected = "object";
    }

    public String getSelected() {
        return selected;
    }
}
