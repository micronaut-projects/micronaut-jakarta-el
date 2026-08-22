package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;
import jakarta.validation.constraints.Size;

/**
 * A bean: its executable methods get environment aware annotation metadata once the context is running, which
 * is the metadata a validator reads for a method parameter.
 */
@Singleton
public class OrderService {

    @Executable
    @Size(min = 5, message = "result ${validatedValue.toUpperCase()} must be at least {min}")
    public String save(@Size(min = 5, message = "value ${validatedValue.toUpperCase()} must be at least {min}") String dollar,
                     @Size(min = 5, message = "value #{validatedValue.toUpperCase()} must be at least {min}") String hash,
                     @Named("${some.unknown.property}") String named) {
        return dollar;
    }
}
