package io.micronaut.el.interpreter;

import java.io.IOException;

/**
 * A class whose constructor declares a checked exception and takes an interface the arguments of the compiler
 * regressions are only coercible to at runtime. The compile-time counterpart is
 * {@code io.micronaut.el.test.CompilerRegressionResource}.
 */
public final class CompilerRegressionResource {

    private final String name;

    public CompilerRegressionResource(Comparable<?> name) throws IOException {
        if ("fail".equals(name)) {
            throw new IOException("checked");
        }
        this.name = "resource:" + name;
    }

    public String getName() {
        return name;
    }
}
