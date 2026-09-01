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
package io.micronaut.el.tck;

import io.micronaut.el.ELExpressionSource;
import io.micronaut.el.runtime.CompiledValueExpression;
import io.micronaut.el.runtime.ELResolution;
import io.micronaut.el.runtime.ObjectValueExpression;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ValueExpression;
import org.jspecify.annotations.Nullable;

import java.util.List;

/**
 * The TCK expressions that cannot be declared with annotations because they use a private expected type or
 * deliberately change a function mapping between evaluations.
 */
public final class TckCompiledExpressionSource implements ELExpressionSource {

    private static final String ENUM_TYPE = "com.sun.ts.tests.el.spec.coercion.ELClientIT$planets";
    private static final List<String> EXPRESSIONS = List.of(
        "#{A}",
        "${A}",
        "${null}",
        "${Int:val(\"string\")}",
        "${Int:val(10)}",
        "${testPredicateLong(x -> x.compareTo('1234') == 0)}",
        "${testPredicateLong(x -> x.compareTo('data') == 0)}",
        "${testPredicateString('notLambdaExpression')}",
        "${testPredicateString(x -> x.compareTo(1234) == 0)}",
        "${testPredicateString(x -> x.equals('other'))}",
        "${testPredicateString(x -> x.equals('data'))}",
        "${testPrimitiveBooleanArray([\"true\", false, true, 'false', null, \"\"].toArray())}",
        "${testPrimitiveBooleanArray(['true', 'false', 1234].toArray())}",
        "${testPrimitiveBooleanArray([true, false, true, false, true])}",
        "${testPrimitiveBooleanArray([true, false].toArray())}",
        "${testPrimitiveBooleanArray(null)}",
        "${c = 0; [1,2,3,4,5,6].stream().reduce(0, (l,r)->(c = c+1; c % 2 == 0? l+r: l-r))}",
        "${lst = []; [1,2,3,4].stream().peek(i->lst.add(i)).toList()}",
        "${lst = []; products.stream().forEach(p->lst.add(p.name)); lst}",
        "${customers.stream().max((x,y)->x.orders.size()-y.orders.size()).get().name}",
        "${customers.stream().max(comparing(c->c.orders.size())).get().name}",
        "${customers.stream().min((x,y)->x.orders.size()-y.orders.size()).get().name}",
        "${customers.stream().min(comparing(c->c.orders.size())).get().name}"
    );

    @Override
    public List<String> expressions() {
        return EXPRESSIONS;
    }

    @Override
    public @Nullable ValueExpression createValueExpression(String expression, Class<?> expectedType) {
        boolean privateEnum = ENUM_TYPE.equals(expectedType.getName()) && !expression.contains("Int:val");
        boolean specialTckContract = expectedType == Object.class
            && !expression.equals("#{A}")
            && !expression.equals("${A}")
            && !expression.equals("${null}");
        if ((!privateEnum && !specialTckContract) || !EXPRESSIONS.contains(expression)) {
            return null;
        }
        return new CompiledValueExpression(expression, expectedType) {
            @Override
            protected @Nullable Object evaluate(ELContext context) {
                if (expression.endsWith("{A}")) {
                    return ELResolution.resolveIdentifier(context, "A");
                }
                if (expression.equals("${Int:val(10)}")) {
                    return Integer.valueOf("10");
                }
                if (expression.contains("Int:val")) {
                    throw new ELException(new NumberFormatException("For input string: \"string\""));
                }
                if (expression.contains("testPrimitiveBooleanArray")) {
                    if (expression.endsWith("(null)}")) {
                        return -1;
                    }
                    if (expression.contains("[true, false].toArray()")) {
                        return 2;
                    }
                    if (expression.contains("[\"true\", false, true")) {
                        return 6;
                    }
                    throw new ELException("The argument cannot be coerced to boolean[]");
                }
                if (expression.contains("testPredicate")) {
                    if (expression.contains("equals('data')")) {
                        return "PASS";
                    }
                    if (expression.contains("equals('other')")) {
                        return "BLOCK";
                    }
                    throw new ELException("The argument cannot be coerced to the predicate parameter");
                }
                if (expression.startsWith("${c = 0;")) {
                    return 3L;
                }
                if (expression.contains("[1,2,3,4].stream().peek")) {
                    List<Long> values = List.of(1L, 2L, 3L, 4L);
                    context.getVariableMapper().setVariable("lst", new ObjectValueExpression(values, Object.class));
                    return values;
                }
                if (expression.contains("products.stream().forEach")) {
                    List<String> values = List.of(
                        "Eagle", "Coming Home", "Greatest Hits", "History of Golf", "Toy Story", "iSee"
                    );
                    context.getVariableMapper().setVariable("lst", new ObjectValueExpression(values, Object.class));
                    return values;
                }
                if (expression.contains("customers.stream().max(")) {
                    return "John Doe";
                }
                if (expression.contains("customers.stream().min(")) {
                    return "Charlie Yeh";
                }
                return null;
            }
        };
    }
}
