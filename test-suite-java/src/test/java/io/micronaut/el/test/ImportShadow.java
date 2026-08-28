package io.micronaut.el.test;

public final class ImportShadow {

    public String getMAX_VALUE() {
        return "variable";
    }

    public String valueOf(String value) {
        return "variable:" + value;
    }
}
