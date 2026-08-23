package example

import io.micronaut.core.annotation.Introspected

@Introspected
data class Customer(val name: String, val age: Int, val country: String)
