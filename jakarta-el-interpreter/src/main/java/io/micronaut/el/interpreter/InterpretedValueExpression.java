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
package io.micronaut.el.interpreter;

import io.micronaut.core.annotation.Internal;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.parser.ELParser;
import io.micronaut.el.parser.ast.ELNode;
import io.micronaut.el.runtime.ELSupport;
import jakarta.el.ELContext;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;
import jakarta.el.ValueReference;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A {@link ValueExpression} evaluating the abstract syntax tree produced by the parser.
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
final class InterpretedValueExpression extends ValueExpression {

    private static final long serialVersionUID = 1L;

    private final String expressionString;
    private final Class<?> expectedType;
    /**
     * Whether the value has to be checked against the sandbox, which a coercion to a string, a boolean, a
     * character, a number or an enum answers on its own.
     */
    private final boolean checkedResult;
    private transient @Nullable ELNode node;
    private transient @Nullable ELInterpreter interpreter;

    InterpretedValueExpression(String expressionString,
                               Class<?> expectedType,
                               ELNode node,
                               ELInterpreter interpreter) {
        this.expressionString = Objects.requireNonNull(expressionString, "expressionString");
        this.expectedType = Objects.requireNonNull(expectedType, "expectedType");
        this.checkedResult = ELSandbox.checksResultOf(expectedType);
        this.node = Objects.requireNonNull(node, "node");
        this.interpreter = Objects.requireNonNull(interpreter, "interpreter");
    }

    @Override
    @Nullable
    @SuppressWarnings("unchecked")
    public <T> T getValue(ELContext context) {
        context.notifyBeforeEvaluation(expressionString);
        Object value = interpreter().evaluateRoot(context, node());
        context.notifyAfterEvaluation(expressionString);
        Object coerced = ELSupport.coerceToType(context, value, expectedType);
        return (T) (checkedResult ? ELSandboxGuard.checkResult(context, coerced) : coerced);
    }

    @Override
    public void setValue(ELContext context, @Nullable Object value) {
        ELInterpreter.Target target = requireTarget(context);
        if (target.base() == null) {
            ELResolutionSupport.setIdentifier(context, target.property(), value);
        } else {
            ELResolutionSupport.setValue(context, target, value);
        }
    }

    @Override
    public boolean isReadOnly(ELContext context) {
        ELInterpreter.Target target = interpreter().resolveTarget(context, node());
        if (target == null) {
            return true;
        }
        return ELResolutionSupport.isReadOnly(context, target);
    }

    @Override
    @Nullable
    public Class<?> getType(ELContext context) {
        ELInterpreter.Target target = interpreter().resolveTarget(context, node());
        if (target == null) {
            return null;
        }
        return ELResolutionSupport.getType(context, target);
    }

    @Override
    @Nullable
    public ValueReference getValueReference(ELContext context) {
        return interpreter().valueReference(context, node());
    }

    @Override
    public Class<?> getExpectedType() {
        return expectedType;
    }

    @Override
    public String getExpressionString() {
        return expressionString;
    }

    @Override
    public boolean isLiteralText() {
        return false;
    }

    private ELNode node() {
        ELNode resolved = node;
        if (resolved == null) {
            resolved = ELParser.parse(expressionString);
            node = resolved;
        }
        return resolved;
    }

    private ELInterpreter interpreter() {
        ELInterpreter resolved = interpreter;
        if (resolved == null) {
            resolved = ELInterpreter.of(null, node());
            interpreter = resolved;
        }
        return resolved;
    }

    @Override
    public boolean equals(@Nullable Object obj) {
        return obj instanceof InterpretedValueExpression other
            && other.node().equals(node())
            && other.expectedType.equals(expectedType);
    }

    @Override
    public int hashCode() {
        return Objects.hash(node(), expectedType);
    }

    @Override
    public String toString() {
        return "ValueExpression[" + expressionString + "]";
    }

    private ELInterpreter.Target requireTarget(ELContext context) {
        ELInterpreter.Target target = interpreter().resolveTarget(context, node());
        if (target == null) {
            throw new PropertyNotWritableException("The expression '" + expressionString + "' is not an lvalue");
        }
        return target;
    }
}
