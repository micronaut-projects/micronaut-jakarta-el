package io.micronaut.el.test.executable;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * A bean that carries both descriptions: the introspection resolves the method, and the call counter shows
 * that it is not resolved a second time through the executable metadata.
 */
@Named("introspected")
@Singleton
@Introspected
public class IntrospectedGreeter {

    private int calls;

    @Executable
    public String greet(String whom) {
        calls++;
        return "hi " + whom;
    }

    public int getCalls() {
        return calls;
    }
}
