package example

import io.micronaut.el.annotation.ELFunction

object TextFunctions {

    @JvmStatic
    @ELFunction // <1>
    fun shout(text: String): String = text.uppercase() + "!"

    @JvmStatic
    @ELFunction("initials") // <2>
    fun initialsOf(text: String): String = text.split(" ").joinToString("") { it.substring(0, 1) }
}
