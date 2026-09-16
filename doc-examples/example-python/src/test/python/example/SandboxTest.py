import java
from jakarta.el import ELManager
from micronaut.el import CompiledELContext, ELSandbox, ELSandboxException
from micronaut.test.extensions.junit5.annotation import MicronautTest
from org.junit.jupiter.api import Test

from example.Book import Book

ELSandboxType = java.type("io.micronaut.el.ELSandbox")
String = java.type("java.lang.String")


@MicronautTest
class SandboxTest:

    @Test
    def test_the_sandbox_can_be_widened(self):
        context = CompiledELContext().setBean("book", Book("Jakarta EL", "reference", 20.0))
        expression = ELManager.getExpressionFactory() \
            .createValueExpression(context, "${book.class.name}", String)  # <1>
        try:
            expression.getValue(context)  # <2>
        except ELSandboxException:
            pass
        else:
            assert False, "the class of the bean should be out of reach"

        context.putContext(ELSandboxType, ELSandbox.UNRESTRICTED)  # <3>
        assert expression.getValue(context) == "example.Book"
