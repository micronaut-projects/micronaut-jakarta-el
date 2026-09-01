package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELMethodExpression;
import io.micronaut.el.annotation.ELVariable;
import jakarta.el.MethodExpression;
import jakarta.el.LambdaExpression;

import java.math.BigDecimal;
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
        @ELVariable(name = "numbers", type = int[].class),
        @ELVariable(name = "functions", type = Formatting.class),
        @ELVariable(name = "f", type = Formatting.class),
        @ELVariable(name = "formatting", type = Formatting.class),
        @ELVariable(name = "integer", type = Integer.class),
        @ELVariable(name = "number", type = Number.class),
        @ELVariable(name = "item", type = Inventory.class),
        @ELVariable(name = "book", type = Book.class),
        @ELVariable(name = "decimal", type = BigDecimal.class),
        @ELVariable(name = "large", type = Long.class),
        @ELVariable(name = "counter", type = EvaluationCounter.class),
        @ELVariable(name = "shadow", type = LambdaExpression.class),
        @ELVariable(name = "twice", type = Long.class),
        @ELVariable(name = "target", type = MethodExpression.class)
    },
    imports = {Varargs.class, VarargsConstructor.class},
    functions = {
        @ELFunctions(prefix = "fn", value = InterpreterParityFunctions.class),
        @ELFunctions(prefix = "alias", value = InterpreterParityFunctions.class),
        @ELFunctions(prefix = "other", value = InterpreterParityFunctions.class),
        @ELFunctions(value = InterpreterParityFunctions.class)
    }
)
@ELExpression(value = "${1 + 2}", name = "addition")
@ELExpression(value = "${1 + 2}", expectedType = String.class, name = "additionAsString")
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
@ELExpression(value = "${ignored -> book.title}", expectedType = LambdaExpression.class, name = "deferredBookTitle")
@ELExpression(value = "${[1,2,3,4].stream().filter(i->i mod 2 == 0).toList()}", name = "filteredStream")
@ELExpression(value = "${[1,2,3,4].stream().sum()}", name = "streamSum")
@ELExpression(value = "${[1,2,3,4].stream().count()}", name = "streamCount")
@ELExpression(value = "${[3,1,2].stream().sorted().toList()}", name = "sortedStream")
@ELExpression(value = "${[1,2,3].stream().max().get()}", name = "streamMaximum")
@ELExpression(value = "${[1].stream().count(1)}", expectedType = Object.class, name = "streamCountWrongArity")
@ELExpression(value = "${[1].stream().filter(x -> true, 2).count()}", expectedType = Object.class,
    name = "streamFilterWrongArity")
@ELExpression(value = "${[1].stream().findFirst().get(1)}", expectedType = Object.class,
    name = "optionalGetWrongArity")
@ELExpression(value = "${[1,2].stream().limit(-1).toList()}", expectedType = Object.class,
    name = "negativeStreamLimit")
@ELExpression(value = "${[1,2].stream().substream(2, 1).toList()}", expectedType = Object.class,
    name = "reversedSubstream")
@ELExpression(value = "${[1].stream().findFirst().ifPresent(x -> x)}", expectedType = Object.class,
    name = "optionalIfPresent")
@ELExpression(value = "${[].stream().findFirst().orElse(null)}", expectedType = Object.class,
    name = "optionalNullFallback")
@ELExpression(value = "${[1].stream().limit(null).toList()}", expectedType = Object.class,
    name = "nullStreamLimit")
@ELExpression(value = "${[1].stream().substream(null).toList()}", expectedType = Object.class,
    name = "nullStreamStart")
@ELExpression(value = "${[1].stream().reduce(null,(a,b)->b)}", expectedType = Object.class,
    name = "nullReductionSeed")
@ELExpression(value = "${null[counter.bump()]}", expectedType = Object.class,
    name = "nullBasePropertyShortCircuit")
@ELExpression(value = "${null.foo(counter.bump())}", expectedType = Object.class,
    name = "nullBaseMethodShortCircuit")
@ELExpression(value = "${book.title}", expectedType = Object.class, name = "nullTypedProperty")
@ELExpression(value = "${book.discounted(counter.bump())}", expectedType = Object.class,
    name = "nullTypedMethod")
@ELExpression(value = "${xs[counter.bump()]}", expectedType = Object.class,
    name = "nullTypedCollection")
@ELExpression(value = "${xs.stream().count()}", expectedType = Object.class,
    name = "nullTypedStream")
@ELExpression(value = "${null[counter.bump()] = counter.bump()}", expectedType = Object.class,
    name = "nullBaseAssignment")
@ELExpression(value = "${greeting}", name = "greeting")
@ELExpression(value = "${greeting.length()}", name = "greetingLength")
@ELExpression(value = "${greeting.toUpperCase()}", name = "uppercaseGreeting")
@ELExpression(value = "${greeting.toUpperCase().substring(0, 3)}", name = "shortUppercaseGreeting")
@ELExpression(value = "${strings.length}", name = "stringsLength")
@ELExpression(value = "${numbers[1]}", name = "primitiveArrayElement")
@ELExpression(value = "${numbers.stream().sum()}", name = "primitiveArrayStreamSum")
@ELExpression(value = "${numbers[1] = Integer.valueOf(4)}", name = "primitiveArrayAssignment")
@ELExpression(value = "${Boolean.TRUE}", name = "booleanConstant")
@ELExpression(value = "${Integer.valueOf(3)}", name = "integerValueOf")
@ELExpression(value = "${String('x')}", name = "stringConstructor")
@ELExpression(value = "${fn:join('a', 'b')}", name = "functionJoin")
@ELExpression(value = "${alias:join('a', 'b')}", expectedType = Object.class, name = "aliasedFunctionJoin")
@ELExpression(value = "${other:joinDifferently('a', 'b')}", expectedType = Object.class,
    name = "differentFunctionJoin")
@ELExpression(value = "${fn:join(sequences)}", name = "functionArrayJoin")
@ELExpression(value = "${fn:join(strings)}", name = "functionSubtypeArrayJoin")
@ELExpression(value = "${join('a', 'b')}", name = "unqualifiedFunctionJoin")
@ELExpression(value = "${Varargs('a', 'b').value}", name = "varargsConstructor")
@ELExpression(value = "${VarargsConstructor('a', 'b').value}", name = "namedVarargsConstructor")
@ELExpression(value = "${varargs.argumentType(varargs.numbers)}", name = "primitiveVarargsArray")
@ELExpression(value = "${varargs.join(strings)}", name = "referenceVarargsArray")
@ELExpression(value = "${strings[0]}", name = "arrayElement")
@ELExpression(value = "${functions.map(value -> value.toUpperCase(), 'el')}", name = "functionalInterface")
@ELExpression(value = "${f.map(value -> value.toUpperCase(), 'el')}", name = "aliasedFunctionalInterface")
@ELExpression(value = "${formatting.argumentType(formatting.numbers)}", name = "formattingPrimitiveArray")
@ELExpression(value = "${formatting.argumentType(strings)}", name = "formattingReferenceArray")
@ELExpression(value = "${Math.max(item.quantity, Integer.valueOf(7))}", name = "staticIntegerMaximum")
@ELExpression(value = "${Math.max(book.unitPrice, 25.0)}", name = "staticDoubleMaximum")
@ELExpression(value = "${decimal == large}", name = "largeIntegralBigDecimalEquality")
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
@ELMethodExpression(value = "#{xs.size}", expectedReturnType = String.class, name = "listSizeStringMethod")
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
