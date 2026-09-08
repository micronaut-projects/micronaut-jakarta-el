package example;

import io.micronaut.el.ELMethodContributor;
import io.micronaut.el.ELMethodRegistry;

public final class BookMethods implements ELMethodContributor { // <1>

    @Override
    public void contribute(ELMethodRegistry registry) {
        registry
            .method(Book.class, "title", String.class, Book::getTitle) // <2>
            .method(Book.class, "discounted", double.class, Double.class, Book::discounted) // <3>
            .method(Book.class, "label", String.class, String.class, Double.class,
                (Book book, String currency, Double percent) -> currency + book.discounted(percent)) // <4>
            .method(Book.class, "tagged", String.class, new Class<?>[]{String.class, String[].class}, true,
                (context, base, arguments) -> ((Book) base).getTitle()
                    + " " + String.join((String) arguments[0], (String[]) arguments[1])) // <5>
            .method(Book.class, "summarised", String.class, Summary.class,
                (Book book, Summary summary) -> summary.of(book)) // <6>
            .staticMethod(Math.class, "abs", long.class, long.class, Math::abs) // <7>
            .constructor(Book.class, new Class<?>[]{String.class, String.class, double.class}, false,
                (context, base, arguments) ->
                    new Book((String) arguments[0], (String) arguments[1], (Double) arguments[2])) // <8>
            .function("fmt", "shout", TextFunctions.class, "shout", String.class, String.class,
                TextFunctions::shout) // <9>
            .functionalInterface(Summary.class,
                (context, lambda) -> book -> String.valueOf(lambda.invoke(context, book))); // <10>
    }

    @Override
    public int getOrder() { // <11>
        return -100;
    }
}
