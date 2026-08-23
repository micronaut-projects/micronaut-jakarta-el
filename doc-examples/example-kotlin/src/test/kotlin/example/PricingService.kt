package example

import io.micronaut.el.annotation.ELFunction
import jakarta.inject.Singleton

@Singleton // <1>
class PricingService {

    private val discount = 0.1

    @ELFunction(prefix = "pricing") // <2>
    fun quote(book: Book, quantity: Int): Double = book.unitPrice * quantity * (1 - discount)

    companion object {
        @JvmStatic
        @ELFunction(prefix = "pricing") // <3>
        fun currency(): String = "EUR"
    }
}
