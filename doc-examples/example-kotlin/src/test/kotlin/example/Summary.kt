package example

/**
 * A functional interface of the application, which an expression can pass a lambda to.
 */
fun interface Summary {

    fun of(book: Book): String
}
