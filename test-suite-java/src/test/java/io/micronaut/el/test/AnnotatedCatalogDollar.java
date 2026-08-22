package io.micronaut.el.test;

import io.micronaut.core.annotation.Introspected;
import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELVariable;

/** The custom annotation written with the {@code ${...}} delimiters of the specification. */
@Introspected
@ELEnvironment(variables = @ELVariable(name = "book", type = Book.class))
@MyNewAnnotation("${ book.title += ' (' += book.category += ')' }")
public class AnnotatedCatalogDollar {
}
