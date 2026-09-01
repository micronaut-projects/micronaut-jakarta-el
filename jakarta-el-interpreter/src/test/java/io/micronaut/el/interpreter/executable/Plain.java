package io.micronaut.el.interpreter.executable;

import java.util.Locale;

/**
 * Neither a bean of the context nor an introspected type: the chain resolves it reflectively, as it does today.
 */
public class Plain {

    public String shout(String whom) {
        return whom.toUpperCase(Locale.ROOT);
    }
}
