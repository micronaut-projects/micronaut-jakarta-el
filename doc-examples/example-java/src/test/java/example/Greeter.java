package example;

import io.micronaut.context.annotation.Executable;
import jakarta.inject.Singleton;

@Singleton
public class Greeter {

    @Executable // <1>
    public String greet(String name) {
        return "Hello " + name;
    }
}
