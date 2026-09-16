from dataclasses import dataclass

from micronaut.context.annotation import Executable
from micronaut.core.annotation import Introspected


@Introspected  # <1>
@dataclass(frozen=True)
class Book:
    title: str  # <2>
    category: str
    unitPrice: float

    @Executable  # <3>
    def discounted(self, percent: float) -> float:
        return self.unitPrice * (100 - percent) / 100
