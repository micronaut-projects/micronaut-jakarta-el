package io.micronaut.el.test

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected
class Book(var title: String, val category: String, var unitPrice: Double) {

    val tags: List<String>
        get() = listOf("new", "sale", "b")

    @Executable
    fun discounted(percent: Double): Double = unitPrice - (unitPrice * percent / 100)

    @Executable
    fun count(predicate: java.util.function.Predicate<String>): Long = tags.stream().filter(predicate).count()
}
