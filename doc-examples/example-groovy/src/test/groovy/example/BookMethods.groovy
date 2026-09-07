package example

import io.micronaut.el.ELMethodContributor
import io.micronaut.el.ELMethodRegistry

class BookMethods implements ELMethodContributor { // <1>

    @Override
    void contribute(ELMethodRegistry registry) {
        registry
            .method(Book, 'title', String, { Book book -> book.title }) // <2>
            .method(Book, 'discounted', double, Double, { Book book, Double percent -> book.discounted(percent) }) // <3>
            .method(Book, 'label', String, String, Double,
                { Book book, String currency, Double percent -> currency + book.discounted(percent) }) // <4>
            .method(Book, 'tagged', String, [String, String[]] as Class<?>[], true,
                { context, base, arguments ->
                    ((Book) base).title + ' ' + String.join((String) arguments[0], (String[]) arguments[1])
                }) // <5>
            .method(Book, 'summarised', String, Summary,
                { Book book, Summary summary -> summary.of(book) }) // <6>
            .staticMethod(Math, 'abs', long, long, { Long value -> Math.abs(value) }) // <7>
            .constructor(Book, [String, String, double] as Class<?>[], false,
                { context, base, arguments ->
                    new Book((String) arguments[0], (String) arguments[1], (Double) arguments[2])
                }) // <8>
            .function('fmt', 'shout', TextFunctions, 'shout', String, String,
                { String text -> TextFunctions.shout(text) }) // <9>
            .functionalInterface(Summary,
                { context, lambda -> { Book book -> String.valueOf(lambda.invoke(context, book)) } as Summary }) // <10>
    }

    @Override
    int getOrder() { // <11>
        -100
    }
}
