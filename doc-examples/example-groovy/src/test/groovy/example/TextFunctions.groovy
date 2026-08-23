package example

import io.micronaut.el.annotation.ELFunction

final class TextFunctions {

    private TextFunctions() {
    }

    static String shout(String text) { // <1>
        text.toUpperCase() + "!"
    }

    @ELFunction("initials") // <2>
    static String initialsOf(String text) {
        text.split(" ").collect { it.charAt(0) }.join()
    }
}
