package io.micronaut.el.test

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELVariable

@ELEnvironment(variables = [ELVariable(name = "book", type = Book::class)])
@ELExpression(value = "\${book.title}", expectedType = String::class, name = "TITLE")
@ELExpression(value = "Book: \${book.title} at \${book.unitPrice}", expectedType = String::class, name = "SUMMARY")
@ELExpression(value = "\${book.discounted(10)}", expectedType = Double::class, name = "DISCOUNTED")
@ELExpression(value = "\${book.unitPrice > 15 ? 'expensive' : 'cheap'}", expectedType = String::class, name = "PRICE_BAND")
class BookExpressions
