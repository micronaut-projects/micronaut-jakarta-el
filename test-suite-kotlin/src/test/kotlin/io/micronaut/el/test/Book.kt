package io.micronaut.el.test

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected
class Book(var title: String, val category: String, var unitPrice: Double) {

    @Executable
    fun discounted(percent: Double): Double = unitPrice - (unitPrice * percent / 100)
}
