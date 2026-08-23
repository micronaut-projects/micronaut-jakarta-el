package example

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected // <1>
class Book(val title: String, val category: String, val unitPrice: Double) { // <2>

    @Executable // <3>
    fun discounted(percent: Double): Double = unitPrice * (100 - percent) / 100
}
