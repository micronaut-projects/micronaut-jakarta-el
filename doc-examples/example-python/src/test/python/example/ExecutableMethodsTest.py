from typing import Annotated

from java.lang import String
from jakarta.el import ELManager
from jakarta.inject import Inject
from micronaut.context import ApplicationContext, BeanDefinitionRegistry
from micronaut.el import CompiledELContext
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Greeter import Greeter


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
