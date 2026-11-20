package io.micronaut.el.interpreter;

import java.io.IOException;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * The bean of the compiler regressions: its methods record what they are called with, so the tests see which
 * operands an expression evaluates, how many times and in which order. The compile-time counterpart is
 * {@code io.micronaut.el.test.CompilerRegressionProbe}.
 */
public class CompilerRegressionProbe {

    /**
     * What the methods and the functions were called with, in order.
     */
    public static final List<String> LOG = new ArrayList<>();

    public String hit(String value) {
        LOG.add(value);
        return value;
    }

    public boolean yes(String value) {
        LOG.add(value);
        return true;
    }

    public boolean isFlag() {
        return true;
    }

    public int getPages() {
        return 300;
    }

    public String getTitle() {
        return "EL";
    }

    public CompilerRegressionProbe getNullBean() {
        return null;
    }

    public List<String> getTags() {
        return List.of("a", "b", "c");
    }

    public List<CompilerRegressionProbe> getProbes() {
        return List.of(this, this);
    }

    public String[] getCodes() {
        return new String[]{"p", "q"};
    }

    public CharSequence getCharSequence() {
        return "cs";
    }

    public Number getNumber() {
        return 5;
    }

    public Optional<String> getOptionalTitle() {
        return Optional.of("opt");
    }

    public Optional<String> getEmptyTitle() {
        return Optional.empty();
    }

    public String takeString(String value) {
        return "str:" + value;
    }

    public String join(String... parts) {
        return String.join("+", parts) + "#" + parts.length;
    }

    public static String stat() {
        return "static";
    }

    public String boom() {
        throw new IllegalStateException("boom");
    }

    public String checked(String value) throws IOException {
        if ("fail".equals(value)) {
            throw new IOException("checked");
        }
        return value;
    }

    public String supply(Supplier<String> supplier) {
        return "got:" + supplier.get();
    }

    public String fn(Function<String, String> function) {
        return "got:" + function.apply("in");
    }

    public String nullable(int value) {
        return "int";
    }

    public String nullable(Object value) {
        return "object";
    }

    public String takeSerializable(Serializable value) {
        return "ser:" + value;
    }
}
