package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import jakarta.el.ELException;

/**
 * An introspected bean whose accessors and methods fail, either with an {@link ELException} or with another
 * runtime exception.
 */
@Introspected
public class FailingBook {

    public String getBroken() {
        throw new IllegalStateException("broken getter");
    }

    public void setBroken(String value) {
        throw new IllegalStateException("broken setter");
    }

    public String getRejected() {
        throw new ELException("rejected getter");
    }

    public void setRejected(String value) {
        throw new ELException("rejected setter");
    }

    @Executable
    public String fail() {
        throw new IllegalStateException("broken method");
    }

    @Executable
    public String reject() {
        throw new ELException("rejected method");
    }
}
