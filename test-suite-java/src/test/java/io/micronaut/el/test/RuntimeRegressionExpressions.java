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
import io.micronaut.el.annotation.ELVariable;

import java.io.File;

/**
 * Compile-time counterparts of the interpreted regressions of
 * {@code io.micronaut.el.interpreter.RuntimeRegressionInterpretedTest}, with the same expression text and the same
 * expected results.
 *
 * <p>The receivers are not declared, so the expressions resolve them dynamically through the resolver chain of the
 * context, which is the runtime path these regressions are about. {@code path} is declared, as the compiler selects
 * a constructor by the type of its argument.</p>
 *
 * <p>The expressions the sandbox denies when parsed at runtime are compiled here as source of the application,
 * which is never sandboxed: they evaluate. The ones whose effect would be harmful even in a test - the beans
 * decoder starting a process, the network and management lookups - and the bare denied identifiers such as
 * {@code ${Class}}, which name no value a compiled expression produces, have no counterpart. Neither has
 * {@code ${String(FileInputStream(path).readAllBytes())}}: the code the compiler generates for it does not compile,
 * since it calls a constructor and a method that declare checked exceptions without handling them.</p>
 */
@ELEnvironment(
    variables = {
        @ELVariable(name = "path", type = String.class),
        @ELVariable(name = "s", type = String.class)
    },
    imports = File.class
)
@ELExpression(value = "${base.hello()}", expectedType = Object.class, name = "baseHello")
@ELExpression(value = "${sub.hello()}", expectedType = Object.class, name = "subHello")
@ELExpression(value = "${base.subOnly()}", expectedType = Object.class, name = "baseSubOnly")
@ELExpression(value = "${bean.count = 7; bean.count}", expectedType = Object.class, name = "assignCount")
@ELExpression(value = "${bean.count = null; bean.count}", expectedType = Object.class, name = "assignNullCount")
@ELExpression(value = "${bean.name = 5; bean.name}", expectedType = Object.class, name = "assignName")
@ELExpression(value = "${bean.unit = 'SECONDS'; bean.unit}", expectedType = Object.class, name = "assignUnit")
@ELExpression(value = "${bean.sarr[0] = 5; bean.sarr[0]}", expectedType = Object.class, name = "assignStringElement")
@ELExpression(value = "${bean.ints[0] = 5; bean.ints[0]}", expectedType = Object.class, name = "assignIntElement")
@ELExpression(value = "${bean.count = 'x'}", expectedType = Object.class, name = "assignInvalidCount")
@ELExpression(value = "${bean.ints[0] = 'x'}", expectedType = Object.class, name = "assignInvalidElement")
@ELExpression(value = "${bean.count}", expectedType = Object.class, name = "count")
@ELExpression(value = "${optS.length()}", expectedType = Object.class, name = "optionalLength")
@ELExpression(value = "${optE.length()}", expectedType = Object.class, name = "emptyOptionalLength")
@ELExpression(value = "${optB.name}", expectedType = Object.class, name = "optionalName")
@ELExpression(value = "${optE.name}", expectedType = Object.class, name = "emptyOptionalName")
@ELExpression(value = "${m.plus(1)}", expectedType = Object.class, name = "contributedPlus")
@ELExpression(value = "${m.two('x')}", expectedType = Object.class, name = "contributedTwo")
@ELExpression(value = "${m.two(1, 2)}", expectedType = Object.class, name = "realTwo")
@ELExpression(value = "${person.boom}", expectedType = Object.class, name = "throwingGetter")
@ELExpression(value = "${sub.fail()}", expectedType = Object.class, name = "throwingMethod")
@ELExpression(value = "${File(path).exists()}", expectedType = Object.class, name = "fileExists")
@ELExpression(value = "${Boolean.getBoolean('micronaut.el.regression')}", expectedType = Object.class,
    name = "booleanProperty")
@ELExpression(value = "${Integer.getInteger('micronaut.el.regression')}", expectedType = Object.class,
    name = "integerProperty")
@ELExpression(value = "${Long.getLong('micronaut.el.regression')}", expectedType = Object.class,
    name = "longProperty")
@ELExpression(value = "${pojo.zone.ID}", expectedType = Object.class, name = "zoneId")
@ELExpression(value = "${pojo.cs.name()}", expectedType = Object.class, name = "charsetName")
@ELExpression(value = "${true lt 'zzz'}", expectedType = Object.class, name = "booleanLessThanString")
@ELExpression(value = "${false gt '9'}", expectedType = Object.class, name = "booleanGreaterThanString")
@ELExpression(value = "${-Character.valueOf(49)}", expectedType = Object.class, name = "negatedCharacter")
@ELExpression(value = "${arr.length}", expectedType = Object.class, name = "arrayLength")
@ELExpression(value = "${bean.big().parallelStream().map(x -> x + 0).toList()}", expectedType = Object.class,
    name = "parallelLambda")
@ELExpression(value = "${y -> s += y}", expectedType = Object.class, name = "escapingLambda")
public final class RuntimeRegressionExpressions {

    private RuntimeRegressionExpressions() {
    }
}
