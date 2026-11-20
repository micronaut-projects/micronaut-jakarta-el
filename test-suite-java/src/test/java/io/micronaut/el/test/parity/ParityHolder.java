package io.micronaut.el.test.parity;

/**
 * The generated twin of {@code ELInterpreterTest.Holder}: a bean with a writable property, so that an lvalue has
 * something to resolve to.
 */
public final class ParityHolder {

    private String name = "n";

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
