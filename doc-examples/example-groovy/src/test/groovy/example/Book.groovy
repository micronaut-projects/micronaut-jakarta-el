package example

import io.micronaut.context.annotation.Executable
import io.micronaut.core.annotation.Introspected

@Introspected // <1>
class Book {
    final String title // <2>
    final String category
    final double unitPrice

    Book(String title, String category, double unitPrice) {
        this.title = title
        this.category = category
        this.unitPrice = unitPrice
    }

    @Executable // <3>
    double discounted(double percent) {
        unitPrice * (100 - percent) / 100
    }
}
