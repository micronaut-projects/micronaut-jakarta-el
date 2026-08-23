package example

import jakarta.inject.Singleton

@Singleton // <1>
class PricingService {

    private val discount = 0.1

    fun quote(book: Book, quantity: Int): Double = book.unitPrice * quantity * (1 - discount) // <2>

    companion object {
        @JvmStatic
        fun currency(): String = "EUR" // <3>
    }
}
