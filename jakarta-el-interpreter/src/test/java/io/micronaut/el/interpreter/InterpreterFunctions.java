package io.micronaut.el.interpreter;

public final class InterpreterFunctions {
    public String map(TextMapper mapper, String value) {
        return mapper.decorate(value);
    }

    public interface TextMapper {
        String apply(String value);

        default String decorate(String value) {
            return "default:" + apply(value);
        }
    }
}
