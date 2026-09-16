from jakarta.inject import Singleton
from micronaut.context.annotation import Executable


@Singleton
class Greeter:

    @Executable  # <1>
    def greet(self, name: str) -> str:
        return "Hello " + name
