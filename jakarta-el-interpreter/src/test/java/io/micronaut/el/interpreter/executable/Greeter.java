package io.micronaut.el.interpreter.executable;

import io.micronaut.context.annotation.Executable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

import java.io.Serializable;

/**
 * A bean whose methods the compiler was asked to make executable, and which deliberately carries no bean
 * introspection: the expressions reach its methods through the executable metadata of its bean definition.
 */
@Named("greeter")
@Singleton
public class Greeter {

    @Executable
    public String greet(String whom) {
        return "hello " + whom;
    }

    @Executable
    public int twice(int value) {
        return value * 2;
    }

    @Executable
    public String select(Integer value) {
        return "integer";
    }

    @Executable
    public String select(String value) {
        return "string";
    }

    @Executable
    public String ambiguous(Comparable<?> value) {
        return "comparable";
    }

    @Executable
    public String ambiguous(Serializable value) {
        return "serializable";
    }

    /**
     * Not executable: the resolver declines it and the reflective resolver of the chain invokes it.
     */
    public String hidden(String whom) {
        return "hidden " + whom;
    }
}
