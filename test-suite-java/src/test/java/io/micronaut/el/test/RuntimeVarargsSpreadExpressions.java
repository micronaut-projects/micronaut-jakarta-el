/*
 * Copyright 2017-2026 original authors
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 * https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package io.micronaut.el.test;

import io.micronaut.el.annotation.ELEnvironment;
import io.micronaut.el.annotation.ELExpression;
import io.micronaut.el.annotation.ELFunctions;
import io.micronaut.el.annotation.ELVariable;

/**
 * Compile-time counterparts of the interpreted tests of
 * {@code io.micronaut.el.interpreter.RuntimeVarargsSpreadTest}, with the same expression text and the same expected
 * results: a single array assignable to a variable arity parameter {@code T...} is the variable arity array, as
 * in the first phase of the section 15.12.2 of the Java Language Specification, which the section 1.2.1.2 of the
 * Jakarta Expression Language specification defers to.
 *
 * <p>The receivers are not declared, so the expressions resolve them dynamically, which is the runtime path the
 * interpreted tests are about. The {@code String[]} is declared, as the compiler selects a constructor, a static
 * method and a function by the type of its argument: those expressions are selected at compile time.</p>
 */
@ELEnvironment(
    variables = @ELVariable(name = "strs", type = String[].class),
    imports = RuntimeSpreadConstructed.class,
    functions = @ELFunctions(prefix = "spread", value = RuntimeSpreadFunctions.class)
)
@ELExpression(value = "${String.format('%s-%s', strs)}", expectedType = Object.class, name = "staticFormat")
@ELExpression(value = "${plain.argumentType(strs)}", expectedType = Object.class, name = "subtypeArray")
@ELExpression(value = "${plain.argumentType(objs)}", expectedType = Object.class, name = "exactArray")
@ELExpression(value = "${plain.argumentType('a', 'b')}", expectedType = Object.class, name = "looseArguments")
@ELExpression(value = "${plain.join('-', strs)}", expectedType = Object.class, name = "fixedThenSubtypeArray")
@ELExpression(value = "${plain.argumentType(ints)}", expectedType = Object.class, name = "primitiveArray")
@ELExpression(value = "${plain.argumentType(null)}", expectedType = Object.class, name = "nullArgument")
@ELExpression(value = "${plain.pick(strs)}", expectedType = Object.class, name = "variableArityOverload")
@ELExpression(value = "${RuntimeSpreadConstructed(strs).value}", expectedType = Object.class,
    name = "constructor")
@ELExpression(value = "${spread:argumentType(strs)}", expectedType = Object.class, name = "function")
@ELExpression(value = "${introspected.argumentType(strs)}", expectedType = Object.class, name = "introspected")
@ELExpression(value = "${executable.argumentType(strs)}", expectedType = Object.class, name = "executable")
public final class RuntimeVarargsSpreadExpressions {

    private RuntimeVarargsSpreadExpressions() {
    }
}
