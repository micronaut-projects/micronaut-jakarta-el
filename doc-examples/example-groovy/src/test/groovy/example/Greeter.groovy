package example

import io.micronaut.context.annotation.Executable
import jakarta.inject.Singleton

@Singleton
class Greeter {

    @Executable // <1>
    String greet(String name) {
        "Hello " + name
    }
}
