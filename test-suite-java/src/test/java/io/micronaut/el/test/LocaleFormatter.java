package io.micronaut.el.test;

import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;

import java.util.Formatter;
import java.util.Locale;

/** The formatter the specification makes available to message templates. */
@Introspected
public record LocaleFormatter(Locale locale) {

    @Executable
    public String format(String format, Object... args) {
        try (Formatter formatter = new Formatter(locale)) {
            return formatter.format(format, args).toString();
        }
    }
}
