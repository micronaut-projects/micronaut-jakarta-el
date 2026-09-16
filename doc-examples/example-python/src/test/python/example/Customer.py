from dataclasses import dataclass

from micronaut.core.annotation import Introspected


@Introspected
@dataclass(frozen=True)
class Customer:
    name: str
    age: int
    country: str
