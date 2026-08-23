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
package io.micronaut.el.runtime;

import io.micronaut.core.annotation.Internal;
import org.jspecify.annotations.Nullable;
import jakarta.el.ELClass;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ImportHandler;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.el.PropertyNotFoundException;
import jakarta.el.PropertyNotWritableException;
import jakarta.el.ValueExpression;
import jakarta.el.VariableMapper;

import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;

/**
 * The resolution of identifiers, properties and methods described in the sections 1.5 and 1.6 of the
 * Jakarta Expression Language specification.
 *
 * <p>This class is invoked by the expressions generated at compilation time, it is not part of the public
 * API of the module.</p>
 *
 * @author Denis Stepanov
 * @since 1.0
 */
@Internal
public final class ELResolution {

    private ELResolution() {
    }

    /**
     * Evaluates an identifier as described in the section 1.5.1 of the specification.
     *
     * @param context The context
     * @param name    The identifier
     * @return The value of the identifier
     */
    @Nullable
    public static Object resolveIdentifier(ELContext context, String name) {
        if (context.isLambdaArgument(name)) {
            return context.getLambdaArgument(name);
        }
        VariableMapper variableMapper = context.getVariableMapper();
        if (variableMapper != null) {
            ValueExpression expression = variableMapper.resolveVariable(name);
            if (expression != null) {
                return expression.getValue(context);
            }
        }
        context.setPropertyResolved(false);
        Object value = context.getELResolver().getValue(context, null, name);
        if (context.isPropertyResolved()) {
            return value;
        }
        ImportHandler importHandler = context.getImportHandler();
        if (importHandler != null) {
            Class<?> staticFieldClass = importHandler.resolveStatic(name);
            if (staticFieldClass != null) {
                return getValueRequired(context, new ELClass(staticFieldClass), name);
            }
            Class<?> resolvedClass = importHandler.resolveClass(name);
            if (resolvedClass != null) {
                return new ELClass(resolvedClass);
            }
        }
        throw new PropertyNotFoundException("Cannot resolve the identifier '" + name + "'");
    }

    /**
     * Assigns a value to an identifier, as used by the assignment operator.
     *
     * @param context The context
     * @param name    The identifier
     * @param value   The value
     */
    public static void setIdentifier(ELContext context, String name, @Nullable Object value) {
        if (context.isLambdaArgument(name)) {
            throw new PropertyNotWritableException("The lambda parameter '" + name + "' is not writable");
        }
        VariableMapper variableMapper = context.getVariableMapper();
        if (variableMapper != null) {
            ValueExpression expression = variableMapper.resolveVariable(name);
            if (expression != null) {
                expression.setValue(context, value);
                return;
            }
        }
        context.setPropertyResolved(false);
        context.getELResolver().setValue(context, null, name, value);
        if (!context.isPropertyResolved()) {
            throw new PropertyNotFoundException("Cannot resolve the identifier '" + name + "'");
        }
    }

    /**
     * @param context The context
     * @param name    The identifier
     * @return The type of the identifier
     */
    @Nullable
    public static Class<?> getIdentifierType(ELContext context, String name) {
        VariableMapper variableMapper = context.getVariableMapper();
        if (variableMapper != null) {
            ValueExpression expression = variableMapper.resolveVariable(name);
            if (expression != null) {
                return expression.getType(context);
            }
        }
        context.setPropertyResolved(false);
        Class<?> type = context.getELResolver().getType(context, null, name);
        if (context.isPropertyResolved()) {
            return type;
        }
        throw new PropertyNotFoundException("Cannot resolve the identifier '" + name + "'");
    }

    /**
     * @param context The context
     * @param name    The identifier
     * @return True if the identifier is read only
     */
    public static boolean isIdentifierReadOnly(ELContext context, String name) {
        if (context.isLambdaArgument(name)) {
            return true;
        }
        VariableMapper variableMapper = context.getVariableMapper();
        if (variableMapper != null) {
            ValueExpression expression = variableMapper.resolveVariable(name);
            if (expression != null) {
                return expression.isReadOnly(context);
            }
        }
        context.setPropertyResolved(false);
        boolean readOnly = context.getELResolver().isReadOnly(context, null, name);
        if (context.isPropertyResolved()) {
            return readOnly;
        }
        throw new PropertyNotFoundException("Cannot resolve the identifier '" + name + "'");
    }

    /**
     * Resolves a property of a base object, returning {@code null} when the base or the property is
     * {@code null} as described for the {@code getValue} evaluation of the section 1.6 of the specification.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The resolved value
     */
    @Nullable
    public static Object getValue(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            return null;
        }
        return getValueRequired(context, base, property);
    }

    /**
     * Resolves a property of a base object, failing when the base or the property is {@code null}.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The resolved value
     */
    @Nullable
    public static Object getValueRequired(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        Object value = context.getELResolver().getValue(context, base, property);
        if (context.isPropertyResolved()) {
            return value;
        }
        throw propertyNotFound(base, property);
    }

    /**
     * Assigns a value to a property of a base object.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @param value    The value to assign
     */
    public static void setValue(ELContext context,
                                @Nullable Object base,
                                @Nullable Object property,
                                @Nullable Object value) {
        if (base == null || property == null) {
            throw propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        context.getELResolver().setValue(context, base, property, value);
        if (!context.isPropertyResolved()) {
            throw propertyNotFound(base, property);
        }
    }

    /**
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return The type of the property
     */
    @Nullable
    public static Class<?> getType(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        Class<?> type = context.getELResolver().getType(context, base, property);
        if (context.isPropertyResolved()) {
            return type;
        }
        throw propertyNotFound(base, property);
    }

    /**
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @return True if the property is read only
     */
    public static boolean isReadOnly(ELContext context, @Nullable Object base, @Nullable Object property) {
        if (base == null || property == null) {
            throw propertyNotFound(base, property);
        }
        context.setPropertyResolved(false);
        boolean readOnly = context.getELResolver().isReadOnly(context, base, property);
        if (context.isPropertyResolved()) {
            return readOnly;
        }
        throw propertyNotFound(base, property);
    }

    /**
     * Assigns a value to an identifier and returns the assigned value, as described in the section 1.13 of
     * the specification.
     *
     * @param context The context
     * @param name    The identifier
     * @param value   The value
     * @return The assigned value
     */
    @Nullable
    public static Object assignIdentifier(ELContext context, String name, @Nullable Object value) {
        setIdentifier(context, name, value);
        return value;
    }

    /**
     * Assigns a value to a property and returns the assigned value, as described in the section 1.13 of the
     * specification.
     *
     * @param context  The context
     * @param base     The base object
     * @param property The property
     * @param value    The value
     * @return The assigned value
     */
    @Nullable
    public static Object assignProperty(ELContext context,
                                        @Nullable Object base,
                                        @Nullable Object property,
                                        @Nullable Object value) {
        setValue(context, base, property, value);
        return value;
    }

    /**
     * Invokes a method expression resolved from a single identifier, as described in the section 1.5.4 of
     * the specification.
     *
     * @param context   The context
     * @param target    The resolved identifier
     * @param arguments The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invokeMethodExpression(ELContext context,
                                                @Nullable Object target,
                                                @Nullable Object[] arguments) {
        if (target instanceof MethodExpression methodExpression) {
            return methodExpression.invoke(context, arguments);
        }
        if (target instanceof LambdaExpression lambdaExpression) {
            lambdaExpression.setELContext(context);
            return lambdaExpression.invoke(context, arguments == null ? new Object[0] : arguments);
        }
        throw new MethodNotFoundException("The identifier does not evaluate to a method expression");
    }

    /**
     * Invokes a method of a base object as described in the section 1.6 of the specification.
     *
     * @param context   The context
     * @param base      The base object
     * @param method    The method name
     * @param arguments The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invoke(ELContext context,
                                @Nullable Object base,
                                @Nullable Object method,
                                Object... arguments) {
        if (base == null || method == null) {
            return null;
        }
        if (base instanceof LambdaExpression lambda && "invoke".equals(method)) {
            lambda.setELContext(context);
            return lambda.invoke(context, arguments);
        }
        context.setPropertyResolved(false);
        Object result = context.getELResolver().invoke(context, base, method, null, arguments);
        if (context.isPropertyResolved()) {
            return result;
        }
        throw new MethodNotFoundException("Cannot find the method '" + method + "' of "
            + base.getClass().getName());
    }

    /**
     * Invokes a method of a base object with the parameters passed to
     * {@code jakarta.el.MethodExpression.invoke(ELContext, Object[])}.
     *
     * @param context  The context
     * @param base     The base object
     * @param method   The method name
     * @param params   The parameters, can be {@code null}
     * @return The result of the invocation
     */
    @Nullable
    public static Object invokeWithParams(ELContext context,
                                          @Nullable Object base,
                                          @Nullable Object method,
                                          @Nullable Object[] params) {
        return invoke(context, base, method, params == null ? new Object[0] : params);
    }

    /**
     * Invokes a method of a base object with the parameter types provided when the expression was created,
     * which is how the section 1.6 of the specification resolves an overloaded method.
     *
     * @param context    The context
     * @param base       The base object
     * @param method     The method name
     * @param paramTypes The parameter types provided at parse time, can be {@code null}
     * @param params     The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invokeWithParamTypes(ELContext context,
                                              @Nullable Object base,
                                              @Nullable Object method,
                                              Class<?> @Nullable [] paramTypes,
                                              @Nullable Object[] params) {
        if (base == null || method == null) {
            throw propertyNotFound(base, method);
        }
        Object[] arguments = params == null ? new Object[0] : params;
        if (base instanceof LambdaExpression lambda) {
            lambda.setELContext(context);
            return lambda.invoke(context, arguments);
        }
        context.setPropertyResolved(false);
        Object result = context.getELResolver().invoke(context, base, method, paramTypes, arguments);
        if (context.isPropertyResolved()) {
            return result;
        }
        throw new MethodNotFoundException("Cannot find the method '" + method + "' of "
            + base.getClass().getName());
    }

    /**
     * Invokes a method found on the base object, which is how the section 1.6 of the specification invokes a
     * method expression that does not provide its own parameters.
     *
     * @param context   The context
     * @param base      The base object
     * @param method    The method
     * @param arguments The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invokeMethod(ELContext context,
                                      Object base,
                                      Method method,
                                      Object @Nullable [] arguments) {
        Class<?>[] parameterTypes = method.getParameterTypes();
        Object[] provided = arguments == null ? new Object[0] : arguments;
        if (provided.length != parameterTypes.length) {
            throw new IllegalArgumentException("The method '" + method.getName() + "' expects "
                + parameterTypes.length + " argument(s) but " + provided.length + " were provided");
        }
        Object[] coerced = new Object[parameterTypes.length];
        for (int i = 0; i < parameterTypes.length; i++) {
            coerced[i] = ELSupport.coerceToType(context, provided[i], parameterTypes[i]);
        }
        try {
            return method.invoke(base, coerced);
        } catch (IllegalAccessException e) {
            throw new ELException("Cannot invoke the method '" + method.getName() + "'", e);
        } catch (InvocationTargetException e) {
            Throwable cause = e.getCause();
            if (cause instanceof ELException elException) {
                throw elException;
            }
            throw new ELException("The method '" + method.getName() + "' failed", cause);
        }
    }

    /**
     * Invokes the result of a function expression as described in the section 1.5.2 of the specification,
     * which covers the invocation of lambda expressions returning lambda expressions.
     *
     * @param context   The context
     * @param target    The evaluated function name
     * @param arguments The arguments
     * @return The result of the invocation
     */
    @Nullable
    public static Object invokeCallable(ELContext context,
                                        @Nullable Object target,
                                        Object... arguments) {
        if (target instanceof LambdaExpression lambda) {
            lambda.setELContext(context);
            return lambda.invoke(context, arguments);
        }
        if (target instanceof ELClass elClass) {
            return newInstance(context, elClass, arguments);
        }
        throw new MethodNotFoundException("The expression does not evaluate to an invocable value: " + target);
    }

    /**
     * Invokes the constructor of an imported class as described in the section 1.24.3 of the specification.
     *
     * @param context   The context
     * @param elClass   The class
     * @param arguments The arguments
     * @return The new instance
     */
    public static Object newInstance(ELContext context,
                                     ELClass elClass,
                                     Object... arguments) {
        context.setPropertyResolved(false);
        Object result = context.getELResolver().invoke(context, elClass, "<init>", null, arguments);
        if (context.isPropertyResolved()) {
            return result;
        }
        throw new MethodNotFoundException("Cannot find a constructor of " + elClass.getKlass().getName()
            + " accepting " + arguments.length + " argument(s)");
    }

    /**
     * @param context The context
     * @param base    The base object
     * @param method  The method name
     * @return The static method or field reference of an imported class
     */
    @Nullable
    public static Object resolveStaticMember(ELContext context,
                                             ELClass base,
                                             String method) {
        return getValueRequired(context, base, method);
    }

    private static PropertyNotFoundException propertyNotFound(@Nullable Object base, @Nullable Object property) {
        if (base == null) {
            return new PropertyNotFoundException("Cannot resolve the property '" + property + "' of a null base object");
        }
        return new PropertyNotFoundException("Cannot resolve the property '" + property + "' of "
            + base.getClass().getName());
    }
}
