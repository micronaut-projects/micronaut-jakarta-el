package example;

/**
 * A functional interface of the application, which an expression can pass a lambda to.
 */
public interface Summary {

    String of(Book book);
}
