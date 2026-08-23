package example;

import io.micronaut.el.annotation.ELFunction;

public final class TextFunctions {

    private TextFunctions() {
    }

    @ELFunction // <1>
    public static String shout(String text) {
        return text.toUpperCase() + "!";
    }

    @ELFunction("initials") // <2>
    public static String initialsOf(String text) {
        StringBuilder initials = new StringBuilder();
        for (String word : text.split(" ")) {
            initials.append(word.charAt(0));
        }
        return initials.toString();
    }
}
