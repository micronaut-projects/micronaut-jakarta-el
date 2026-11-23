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
package io.micronaut.el.interpreter.parity;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import jakarta.el.ELContext;
import jakarta.el.ExpressionFactory;
import jakarta.el.FunctionMapper;
import jakarta.el.MethodExpression;
import jakarta.el.ValueExpression;

import java.lang.reflect.Method;
import java.util.HashMap;
import java.util.Map;

/**
 * Evaluates the text of a generated regression expression through the interpreter, in the same kind of context the
 * generated test evaluates it in: a {@link CompiledELContext}, with the beans the generated test sets. No
 * expression is compiled in this module, so the factory hands every text but a literal one to the interpreter.
 */
final class ParityEvaluation {

    private static final ExpressionFactory FACTORY = new CompiledExpressionFactory();

    private ParityEvaluation() {
    }

    static ExpressionFactory factory() {
        return FACTORY;
    }

    static ValueExpression expression(ELContext context, String text, Class<?> expectedType) {
        return FACTORY.createValueExpression(context, text, expectedType);
    }

    static Object value(ELContext context, String text) {
        return expression(context, text, Object.class).getValue(context);
    }

    static Object value(ELContext context, String text, Class<?> expectedType) {
        return expression(context, text, expectedType).getValue(context);
    }

    static MethodExpression method(ELContext context, String text, Class<?> expectedReturnType,
                                   Class<?>[] expectedParamTypes) {
        return FACTORY.createMethodExpression(context, text, expectedReturnType, expectedParamTypes);
    }

    /**
     * A compiled context whose function mapper knows the functions a generated environment declares.
     */
    static final class FunctionContext extends CompiledELContext {

        private final Map<String, Method> functions = new HashMap<>();

        FunctionContext function(String prefix, String localName, Method method) {
            functions.put(prefix + ":" + localName, method);
            return this;
        }

        @Override
        public FunctionMapper getFunctionMapper() {
            return new FunctionMapper() {
                @Override
                public Method resolveFunction(String prefix, String localName) {
                    return functions.get(prefix + ":" + localName);
                }
            };
        }
    }
}
