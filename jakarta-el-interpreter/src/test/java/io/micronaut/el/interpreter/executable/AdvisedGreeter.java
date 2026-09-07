package io.micronaut.el.interpreter.executable;

import io.micronaut.context.annotation.Executable;
import jakarta.inject.Named;
import jakarta.inject.Singleton;

/**
 * An AOP-advised bean: the runtime class of the instance is the generated proxy, whose definition is a
 * {@code ProxyBeanDefinition}, so the resolver has to read through to the executable methods of this class.
 */
@Named("advised")
@Singleton
@Shouted
public class AdvisedGreeter {

    @Executable
    public String greet(String whom) {
        return "hello " + whom;
    }
}
