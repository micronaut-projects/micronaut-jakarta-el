from java.lang import Boolean, String
from jakarta.el import ELManager
from jakarta.inject import Singleton
from micronaut.aop import InterceptorBean, MethodInterceptor, MethodInvocationContext
from micronaut.el import CompiledELContext
from micronaut.el.example.eligible import ConstraintMessages, Eligible, MinAmount

from example.NotEligibleException import NotEligibleException

@Singleton
@InterceptorBean(Eligible)  # <1>
class EligibleInterceptor(MethodInterceptor):

    def __init__(self):
        self.expressions = {}

    def intercept(self, context: MethodInvocationContext):
        el_context = CompiledELContext()
        for entry in context.getParameterValueMap().entrySet():  # <2>
            el_context.setBean(entry.getKey(), entry.getValue())

        for argument in context.getArguments():  # <5>
            constraint = argument.getAnnotationMetadata().getAnnotation(MinAmount)
            value = context.getParameterValueMap().get(argument.getName())
            if constraint is not None and isinstance(value, int) and not self.satisfies(constraint, value):
                raise NotEligibleException(ConstraintMessages.interpolate(constraint, value))  # <6>

        condition = context.stringValue(Eligible).orElseThrow()  # <3>
        if self.expression(el_context, condition, Boolean).getValue(el_context) is True:
            return context.proceed()
        otherwise = context.stringValue(Eligible, "otherwise").orElse("")
        if otherwise:
            message = self.expression(el_context, otherwise, String).getValue(el_context)
        else:
            message = context.getMethodName() + " requires " + condition
        raise NotEligibleException(message)

    def satisfies(self, constraint, amount: int) -> bool:
        minimum = constraint.longValue().orElseThrow()
        return amount >= minimum if constraint.booleanValue("inclusive").orElse(False) else amount > minimum

    def expression(self, el_context: CompiledELContext, text: str, expected_type):
        if text not in self.expressions:
            self.expressions[text] = ELManager.getExpressionFactory().createValueExpression(el_context, text, expected_type)  # <4>
        return self.expressions[text]
