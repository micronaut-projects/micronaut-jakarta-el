package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELVariable;

/**
 * The expression is written with the {@code #{...}} delimiters Micronaut would otherwise claim. The type of
 * {@code book} is declared so that the property access compiles to a direct invocation.
 */
@Introspected
@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
@MyNewAnnotation("#{ book.title += ' (' += book.category += ')' }")
public class AnnotatedCatalog {
}
