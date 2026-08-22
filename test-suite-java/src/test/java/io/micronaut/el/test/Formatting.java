package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.util.Arrays;
import java.util.stream.Collectors;

@Introspected
public class Formatting {

    @Executable
    public String join(String separator, Object... parts) {
        return Arrays.stream(parts).map(String::valueOf).collect(Collectors.joining(separator));
    }

    @Executable
    public String join(String separator) {
        return "only:" + separator;
    }

    @Executable
    public int size(Object[] values) {
        return values.length;
    }

    @Executable
    public String format(String format, Object... args) {
        return String.format(format, args);
    }
}
