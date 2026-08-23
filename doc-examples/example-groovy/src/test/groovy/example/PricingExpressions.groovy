package example

import io.micronaut.el.annotation.ELEnvironment
import io.micronaut.el.annotation.ELExpression
import io.micronaut.el.annotation.ELVariable

@ELEnvironment(variables = @ELVariable(name = "book", type = Book)) // <1>
@ELExpression(value = '${pricing:quote(book, 3)}', expectedType = double, name = "QUOTE") // <2>
@ELExpression(value = "\${pricing:quote(book, 3) += ' ' += pricing:currency()}", expectedType = String, name = "PRICED") // <3>
class PricingExpressions {
}
