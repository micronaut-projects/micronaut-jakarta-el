from typing import Annotated

import java
from jakarta.el import ELManager
from jakarta.inject import Inject
from micronaut.context import ApplicationContext
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Greeter import Greeter

BeanDefinitionRegistry = java.type("io.micronaut.context.BeanDefinitionRegistry")
String = java.type("java.lang.String")


@MicronautTest
class ExecutableMethodsTest:

    bean_context: Annotated[ApplicationContext, Inject]
    greeter: Annotated[Greeter, Inject]

    @Test
    def test_an_executable_method_is_invoked_without_reflection(self):
        context = CompiledELContext().setBean("greeter", self.greeter)
        context.putContext(BeanDefinitionRegistry, self.bean_context)  # <1>

        assert ELManager.getExpressionFactory() \
            .createValueExpression(context, "${greeter.greet('world')}", String).getValue(context) == "Hello world"  # <2>
