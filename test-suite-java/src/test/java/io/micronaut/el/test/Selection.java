package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.util.Comparator;

/**
 * The overload selection cases the reflective and the contributed dispatch also cover, so that both execution
 * modes are known to agree on them.
 */
@Introspected
public class Selection {

    @Executable
    public String nullable(int value) {
        return "int";
    }

    @Executable
    public String nullable(Object value) {
        return "object";
    }

    @Executable
    public String sorted(Comparator<Object> comparator) {
        return String.valueOf(comparator.compare("a", "b"));
    }

}
