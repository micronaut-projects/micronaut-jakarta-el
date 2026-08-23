package example

import io.micronaut.el.annotation.ELFunction

final class TextFunctions {

    private TextFunctions() {
    }

    @ELFunction // <1>
    static String shout(String text) {
        text.toUpperCase() + "!"
    }

    @ELFunction("initials") // <2>
    static String initialsOf(String text) {
        text.split(" ").collect { it.charAt(0) }.join()
    }
}
