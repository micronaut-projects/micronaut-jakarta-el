package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

/**
 * The compile-time counterparts of {@code io.micronaut.el.interpreter.CompilerRegressionInterpretedTest}: the
 * same expressions, with the same results, evaluated both ways. Each is a defect the compiler had.
 */
@ELEnvironment(
    variables = @ELVariable(name = "probe", type = CompilerRegressionProbe.class),
    imports = CompilerRegressionResource.class,
    staticImports = CompilerRegressionFunctions.class,
    functions = @ELFunctions(value = CompilerRegressionFunctions.class, prefix = "fn")
)
// the operands of ?:, && and || that are not taken are not evaluated
@ELExpression(value = "${probe.flag ? probe.hit('t').trim() : probe.hit('f').trim()}", expectedType = Object.class, name = "shortTernary")
@ELExpression(value = "${false && probe.hit('a').length() > 0}", expectedType = Object.class, name = "shortAnd")
@ELExpression(value = "${true || probe.yes('c')}", expectedType = Object.class, name = "shortOr")
@ELExpression(value = "${empty probe.tags ? probe.hit('e') : probe.hit('n')}", expectedType = Object.class, name = "shortEmpty")
@ELExpression(value = "${probe.title += (probe.flag ? '' : probe.hit('never'))}", expectedType = Object.class, name = "shortNested")
// an argument is evaluated once
@ELExpression(value = "${probe.takeString(probe.hit('arg'))}", expectedType = Object.class, name = "argumentOnce")
// the operands are evaluated from left to right
@ELExpression(value = "${fn:hitS('x') += probe.hit('y')}", expectedType = Object.class, name = "orderConcat")
@ELExpression(value = "${[fn:hitS('x'), probe.hit('y')]}", expectedType = Object.class, name = "orderList")
@ELExpression(value = "${fn:hitS('x') == probe.hit('y')}", expectedType = Object.class, name = "orderEqual")
// a primitive property read through a null base is null, not zero or false
@ELExpression(value = "${probe.nullBean.pages == 0}", expectedType = Object.class, name = "nullPrimitiveEqual")
@ELExpression(value = "${probe.nullBean.pages < 1}", expectedType = Object.class, name = "nullPrimitiveLess")
@ELExpression(value = "${probe.nullBean.pages <= 0}", expectedType = Object.class, name = "nullPrimitiveLessOrEqual")
@ELExpression(value = "${probe.nullBean.flag == false}", expectedType = Object.class, name = "nullPrimitiveBoolean")
@ELExpression(value = "${probe.probes.stream().filter(p -> p.nullBean.pages == 0).count()}", expectedType = Object.class, name = "nullPrimitiveInLambda")
@ELExpression(value = "${-probe.nullBean.pages}", expectedType = Object.class, name = "nullPrimitiveNegated")
@ELExpression(value = "${probe.nullBean.pages + 1}", expectedType = Object.class, name = "nullPrimitiveAdded")
// a single argument of a variable arity method
@ELExpression(value = "${probe.join(probe.title)}", expectedType = Object.class, name = "varargsSingle")
@ELExpression(value = "${probe.join(probe.codes)}", expectedType = Object.class, name = "varargsArray")
@ELExpression(value = "${fn:vs('a')}", expectedType = Object.class, name = "functionVarargsSingle")
// an identifier referenced only where it is not evaluated is not resolved
@ELExpression(value = "${true || (undefinedVar.a == undefinedVar.b)}", expectedType = Object.class, name = "undefinedNotResolved")
// a lambda parameter holding a '$'
@ELExpression(value = "${probe.tags.stream().map($x -> $x).toList()}", expectedType = Object.class, name = "dollarParameter")
// a static method invoked through an instance
@ELExpression(value = "${probe.stat()}", expectedType = Object.class, name = "staticThroughInstance")
@ELExpression(value = "${probe.title.valueOf(5)}", expectedType = Object.class, name = "staticOverloadThroughInstance")
// a void function evaluates to null
@ELExpression(value = "${fn:nothing()}", expectedType = Object.class, name = "voidFunction")
// arguments the runtime coercion accepts
@ELExpression(value = "${fn:cmp(probe.pages)}", expectedType = Object.class, name = "intToComparable")
@ELExpression(value = "${fn:cmp(probe.number)}", expectedType = Object.class, name = "numberToComparable")
@ELExpression(value = "${fn:ser(probe.flag)}", expectedType = Object.class, name = "booleanToSerializable")
@ELExpression(value = "${fn:cmp(probe.charSequence)}", expectedType = Object.class, name = "charSequenceToComparable")
@ELExpression(value = "${fn:list(probe.charSequence)}", expectedType = Object.class, name = "charSequenceToList")
@ELExpression(value = "${cmp(probe.number)}", expectedType = Object.class, name = "importedStaticCoercion")
@ELExpression(value = "${CompilerRegressionResource(probe.number).name}", expectedType = Object.class, name = "constructorCoercion")
// a lambda returning null to a type variable
@ELExpression(value = "${probe.supply(() -> null)}", expectedType = Object.class, name = "supplierNull")
@ELExpression(value = "${probe.fn(x -> null)}", expectedType = Object.class, name = "functionNull")
// what a member throws is an ELException with the exception as its cause, a checked one included
@ELExpression(value = "${probe.boom()}", expectedType = Object.class, name = "memberThrows")
@ELExpression(value = "${probe.checked('ok')}", expectedType = Object.class, name = "checkedPasses")
@ELExpression(value = "${probe.checked('fail')}", expectedType = Object.class, name = "checkedThrows")
@ELExpression(value = "${fn:read('fail')}", expectedType = Object.class, name = "checkedFunctionThrows")
@ELExpression(value = "${CompilerRegressionResource('fail')}", expectedType = Object.class, name = "checkedConstructorThrows")
// the property and the method of an Optional are the ones of its value
@ELExpression(value = "${probe.optionalTitle.present}", expectedType = Object.class, name = "optionalProperty")
@ELExpression(value = "${probe.optionalTitle.length()}", expectedType = Object.class, name = "optionalMethod")
@ELExpression(value = "${probe.emptyTitle.length()}", expectedType = Object.class, name = "emptyOptionalMethod")
// a null argument selects the primitive overload
@ELExpression(value = "${probe.nullable(null)}", expectedType = Object.class, name = "nullArgumentOverload")
public final class CompilerRegressionExpressions {

    private CompilerRegressionExpressions() {
    }
}
