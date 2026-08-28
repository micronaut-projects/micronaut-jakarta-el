package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;
import jakarta.el.MethodExpression;
import jakarta.el.LambdaExpression;

import java.util.List;

/**
 * Compile-time counterparts of the interpreter regression expressions that share this environment.
 */
@ELEnvironment(
    variables = {
        @ELVariable(name = "greeting", type = String.class),
        @ELVariable(name = "xs", type = List.class),
        @ELVariable(name = "bean", type = Varargs.class),
        @ELVariable(name = "sequences", type = CharSequence[].class),
        @ELVariable(name = "varargs", type = Varargs.class),
        @ELVariable(name = "strings", type = String[].class),
        @ELVariable(name = "functions", type = Formatting.class),
        @ELVariable(name = "integer", type = Integer.class),
        @ELVariable(name = "number", type = Number.class),
        @ELVariable(name = "counter", type = EvaluationCounter.class),
        @ELVariable(name = "shadow", type = LambdaExpression.class),
        @ELVariable(name = "twice", type = Long.class),
        @ELVariable(name = "target", type = MethodExpression.class)
    },
    imports = Varargs.class,
    functions = {
        @ELFunctions(prefix = "fn", value = InterpreterParityFunctions.class),
        @ELFunctions(value = InterpreterParityFunctions.class)
    }
)
@ELExpression(value = "${1 + 2}", name = "addition")
@ELExpression(value = "${5 / 2}", name = "division")
@ELExpression(value = "${7 mod 3}", name = "modulo")
@ELExpression(value = "${-(1 + 2)}", name = "negation")
@ELExpression(value = "${'a' += 'b'}", name = "concatenation")
@ELExpression(value = "${1 lt 2}", name = "lessThan")
@ELExpression(value = "${'a' == 'a'}", name = "equality")
@ELExpression(value = "${null == 1}", name = "nullEquality")
@ELExpression(value = "${null < (-1 < '1.5')}", name = "nullRelationalShortCircuit")
@ELExpression(value = "${null > (-1 < '1.5')}", name = "nullGreaterThanShortCircuit")
@ELExpression(value = "${true and not false}", name = "logical")
@ELExpression(value = "${empty null}", name = "emptyNull")
@ELExpression(value = "${empty []}", name = "emptyList")
@ELExpression(value = "${true ? 'yes' : 'no'}", name = "conditional")
@ELExpression(value = "${1; 2}", name = "semicolon")
@ELExpression(value = "${[1,2,3]}", name = "list")
@ELExpression(value = "${{'one':1}}", name = "map")
@ELExpression(value = "${{1,2,3}}", name = "set")
@ELExpression(value = "${((x,y)->x+y)(3,4)}", name = "immediateLambda")
@ELExpression(value = "${v = (x,y)->x+y; v(3,4)}", name = "assignedLambda")
@ELExpression(value = "${fact = n -> n==0? 1: n*fact(n-1); fact(5)}", name = "factorial")
@ELExpression(value = "${(x->y->x+y)(1)(2)}", name = "nestedLambda")
@ELExpression(value = "${[1,2,3,4].stream().filter(i->i mod 2 == 0).toList()}", name = "filteredStream")
@ELExpression(value = "${[1,2,3,4].stream().sum()}", name = "streamSum")
@ELExpression(value = "${[1,2,3,4].stream().count()}", name = "streamCount")
@ELExpression(value = "${[3,1,2].stream().sorted().toList()}", name = "sortedStream")
@ELExpression(value = "${[1,2,3].stream().max().get()}", name = "streamMaximum")
@ELExpression(value = "${greeting}", name = "greeting")
@ELExpression(value = "${greeting.length()}", name = "greetingLength")
@ELExpression(value = "${greeting.toUpperCase()}", name = "uppercaseGreeting")
@ELExpression(value = "${Boolean.TRUE}", name = "booleanConstant")
@ELExpression(value = "${Integer.valueOf(3)}", name = "integerValueOf")
@ELExpression(value = "${String('x')}", name = "stringConstructor")
@ELExpression(value = "${fn:join('a', 'b')}", name = "functionJoin")
@ELExpression(value = "${fn:join(sequences)}", name = "functionArrayJoin")
@ELExpression(value = "${Varargs('a', 'b').value}", name = "varargsConstructor")
@ELExpression(value = "${varargs.argumentType(varargs.numbers)}", name = "primitiveVarargsArray")
@ELExpression(value = "${varargs.join(strings)}", name = "referenceVarargsArray")
@ELExpression(value = "${strings[0]}", name = "arrayElement")
@ELExpression(value = "${functions.map(value -> value.toUpperCase(), 'el')}", name = "functionalInterface")
@ELExpression(value = "${twice(3)}", name = "mappedFunctionFallback")
@ELExpression(value = "${twice2(3)}", expectedType = Object.class, name = "missingFunction")
@ELExpression(value = "${varargs.choose(1, 1)}", name = "assignableOverCoercible")
@ELExpression(value = "${varargs.specific(1)}", name = "mostSpecificOverload")
@ELExpression(value = "${varargs.boxed(integer)}", name = "boxedOverload")
@ELExpression(value = "${varargs.pick(number)}", name = "runtimeSubtypeOverload")
@ELExpression(value = "${fn:identity(null < counter.bump())}", name = "nestedRelationalShortCircuit")
@ELExpression(value = "${shadow('x')}", name = "lambdaVariableShadowsFunction")
@ELExpression(value = "${varargs.emptyVarargs()}", expectedType = Object.class, name = "emptyVarargsAmbiguity")
@ELExpression(value = "${varargs.numeric(1)}", expectedType = Object.class, name = "numericAmbiguity")
@ELExpression(value = "${varargs.reject(value -> value)}", expectedType = Object.class, name = "nonFunctionalInterface")
@ELExpression(value = "${varargs.rejectSealed(value -> value)}", expectedType = Object.class, name = "sealedInterface")
@ELExpression(value = "${'1'}", expectedType = Integer.class, name = "coercionListenerValue")
@ELMethodExpression(value = "#{xs.size}", expectedReturnType = Object.class, name = "listSizeMethod")
@ELMethodExpression(value = "#{Integer.valueOf}", expectedReturnType = Integer.class,
    expectedParamTypes = String.class, name = "integerValueOfMethod")
@ELMethodExpression(value = "#{bean.join}", expectedReturnType = String.class,
    expectedParamTypes = String[].class, name = "varargsMethod")
@ELMethodExpression(value = "#{bean.argumentType}", expectedReturnType = String.class,
    expectedParamTypes = Object[].class, name = "objectVarargsMethod")
@ELMethodExpression(value = "#{varargs.join('a', 'b')}", expectedReturnType = String.class,
    name = "providedVarargsMethod")
@ELMethodExpression(value = "#{target}", expectedReturnType = Object.class, name = "identifierMethod")
@ELMethodExpression(value = "#{varargs.numberText}", expectedReturnType = Integer.class,
    name = "coercionListenerMethod")
@ELMethodExpression(value = "#{varargs.specific}", expectedReturnType = String.class,
    expectedParamTypes = Number.class, name = "specificMethod")
@ELMethodExpression(value = "#{varargs.compatible}", expectedReturnType = String.class,
    expectedParamTypes = String.class, name = "compatibleMethod")
@ELMethodExpression(value = "#{varargs.expanded}", expectedReturnType = String.class,
    expectedParamTypes = {String.class, String.class}, name = "expandedVarargsMethod")
public final class ELInterpreterExpressions {

    private ELInterpreterExpressions() {
    }
}
