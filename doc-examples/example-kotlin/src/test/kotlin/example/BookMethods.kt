package example

import io.micronaut.el.ELMethodContributor
import io.micronaut.el.ELMethodRegistry

class BookMethods : ELMethodContributor { // <1>

    override fun contribute(registry: ELMethodRegistry) {
        registry
            .method(Book::class.java, "title", String::class.java,
                ELMethodRegistry.Call0 { book: Book -> book.title }) // <2>
            .method(Book::class.java, "discounted", Double::class.javaPrimitiveType!!, Double::class.java,
                ELMethodRegistry.Call1 { book: Book, percent: Double? -> book.discounted(percent!!) }) // <3>
            .method(Book::class.java, "label", String::class.java, String::class.java, Double::class.java,
                ELMethodRegistry.Call2 { book: Book, currency: String?, percent: Double? ->
                    currency + book.discounted(percent!!)
                }) // <4>
            .method(Book::class.java, "tagged", String::class.java,
                arrayOf<Class<*>>(String::class.java, Array<String>::class.java), true,
                { _, base, arguments ->
                    (base as Book).title + " " +
                        java.lang.String.join(arguments[0] as String, *(arguments[1] as Array<String>))
                }) // <5>
            .method(Book::class.java, "summarised", String::class.java, Summary::class.java,
                ELMethodRegistry.Call1 { book: Book, summary: Summary? -> summary!!.of(book) }) // <6>
            .staticMethod(Math::class.java, "abs", Long::class.javaPrimitiveType!!, Long::class.java,
                ELMethodRegistry.Fn1 { value: Long? -> Math.abs(value!!) }) // <7>
            .constructor(Book::class.java,
                arrayOf<Class<*>>(String::class.java, String::class.java, Double::class.javaPrimitiveType!!),
                false,
                { _, _, arguments ->
                    Book(arguments[0] as String, arguments[1] as String, arguments[2] as Double)
                }) // <8>
            .function("fmt", "shout", TextFunctions::class.java, "shout", String::class.java,
                String::class.java,
                ELMethodRegistry.Fn1 { text: String? -> TextFunctions.shout(text!!) }) // <9>
            .functionalInterface(Summary::class.java) { context, lambda ->
                Summary { book -> lambda.invoke(context, book).toString() }
            } // <10>
    }

    override fun getOrder(): Int = -100 // <11>
}
