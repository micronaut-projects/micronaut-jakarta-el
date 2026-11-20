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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.ELMethodContributor;
import io.micronaut.el.ELMethodRegistry;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.LambdaExpression;
import jakarta.el.MethodNotFoundException;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The compile-time generated counterparts of the interpreted runtime regressions of
 * {@code io.micronaut.el.interpreter.RuntimeRegressionInterpretedTest}.
 */
class RuntimeRegressionExpressionsTest {

    private final ApplicationContext beanContext = ApplicationContext.run();

    @AfterEach
    void close() {
        beanContext.close();
    }

    @Test
    void aSuperclassInstanceIsNotDispatchedAsItsBeanSubclass() {
        CompiledELContext context = new CompiledELContext(beanContext)
            .setBean("base", new RegressionBase())
            .setBean("sub", beanContext.getBean(RegressionSub.class));

        assertEquals("base", RuntimeRegressionExpressions$ELExpressions.BASE_HELLO.getValue(context));
        assertEquals("sub", RuntimeRegressionExpressions$ELExpressions.SUB_HELLO.getValue(context));
        assertThrows(MethodNotFoundException.class,
            () -> RuntimeRegressionExpressions$ELExpressions.BASE_SUB_ONLY.getValue(context));
    }

    @Test
    void anAssignmentCoercesTheValueToTheTypeOfTheProperty() {
        RegressionPlain bean = new RegressionPlain();
        CompiledELContext context = new CompiledELContext().setBean("bean", bean);

        assertEquals((Object) 7, RuntimeRegressionExpressions$ELExpressions.ASSIGN_COUNT.getValue(context));
        assertEquals((Object) 0, RuntimeRegressionExpressions$ELExpressions.ASSIGN_NULL_COUNT.getValue(context));
        assertEquals("5", RuntimeRegressionExpressions$ELExpressions.ASSIGN_NAME.getValue(context));
        assertEquals(TimeUnit.SECONDS, RuntimeRegressionExpressions$ELExpressions.ASSIGN_UNIT.getValue(context));
        assertEquals("5", RuntimeRegressionExpressions$ELExpressions.ASSIGN_STRING_ELEMENT.getValue(context));
        assertEquals((Object) 5, RuntimeRegressionExpressions$ELExpressions.ASSIGN_INT_ELEMENT.getValue(context));
        assertArrayEquals(new int[]{5, 0}, bean.getInts());
    }

    @Test
    void aFailedAssignmentIsAnELException() {
        CompiledELContext context = new CompiledELContext().setBean("bean", new RegressionPlain());

        assertThrows(ELException.class,
            () -> RuntimeRegressionExpressions$ELExpressions.ASSIGN_INVALID_COUNT.getValue(context));
        assertThrows(ELException.class,
            () -> RuntimeRegressionExpressions$ELExpressions.ASSIGN_INVALID_ELEMENT.getValue(context));
    }

    @Test
    void setValueCoercesTheValueToTheTypeOfTheProperty() {
        RegressionPlain bean = new RegressionPlain();
        CompiledELContext context = new CompiledELContext().setBean("bean", bean);

        RuntimeRegressionExpressions$ELExpressions.COUNT.setValue(context, "5");

        assertEquals(5, bean.getCount());
    }

    @Test
    void aMethodOfAnOptionalIsTheMethodOfItsValue() {
        CompiledELContext context = new CompiledELContext()
            .setBean("optS", Optional.of("ab"))
            .setBean("optE", Optional.empty())
            .setBean("optB", Optional.of(new RegressionPlain()));

        assertEquals((Object) 2, RuntimeRegressionExpressions$ELExpressions.OPTIONAL_LENGTH.getValue(context));
        assertNull(RuntimeRegressionExpressions$ELExpressions.EMPTY_OPTIONAL_LENGTH.getValue(context));
        assertEquals("plain", RuntimeRegressionExpressions$ELExpressions.OPTIONAL_NAME.getValue(context));
        assertNull(RuntimeRegressionExpressions$ELExpressions.EMPTY_OPTIONAL_NAME.getValue(context));
    }

    @Test
    void aContributedMethodIsFoundFromADynamicallyTypedReceiver() {
        // RegressionMoneyContributor is registered as a service of this test suite
        CompiledELContext context = new CompiledELContext().setBean("m", new RegressionMoney(5));

        assertEquals((Object) 6L, RuntimeRegressionExpressions$ELExpressions.CONTRIBUTED_PLUS.getValue(context));
        assertEquals("contributed-two", RuntimeRegressionExpressions$ELExpressions.CONTRIBUTED_TWO.getValue(context));
        assertEquals("real-two", RuntimeRegressionExpressions$ELExpressions.REAL_TWO.getValue(context));
    }

    @Test
    void whatAGeneratedMemberThrowsIsAnELException() {
        CompiledELContext context = new CompiledELContext(beanContext)
            .setBean("person", new RegressionPerson())
            .setBean("sub", beanContext.getBean(RegressionSub.class));

        ELException getter = assertThrows(ELException.class,
            () -> RuntimeRegressionExpressions$ELExpressions.THROWING_GETTER.getValue(context));
        assertInstanceOf(IllegalStateException.class, getter.getCause());
        ELException method = assertThrows(ELException.class,
            () -> RuntimeRegressionExpressions$ELExpressions.THROWING_METHOD.getValue(context));
        assertInstanceOf(IllegalStateException.class, method.getCause());
    }

    @Test
    void theExpressionsTheSandboxDeniesEvaluateWhenCompiled(@TempDir Path directory) throws Exception {
        // the interpreted counterparts are denied under the standard sandbox, and evaluate to these values without
        Path secret = Files.writeString(directory.resolve("secret.txt"), "secret-content");
        CompiledELContext context = new CompiledELContext().setBean("path", secret.toString());

        assertEquals(true, RuntimeRegressionExpressions$ELExpressions.FILE_EXISTS.getValue(context));
        assertEquals(false, RuntimeRegressionExpressions$ELExpressions.BOOLEAN_PROPERTY.getValue(context));
        assertNull(RuntimeRegressionExpressions$ELExpressions.INTEGER_PROPERTY.getValue(context));
        assertNull(RuntimeRegressionExpressions$ELExpressions.LONG_PROPERTY.getValue(context));
    }

    @Test
    void aValueOfThePlatformIsReadThroughItsPublicType() {
        CompiledELContext context = new CompiledELContext().setBean("pojo", new RegressionPojo());

        assertEquals(TimeZone.getTimeZone("UTC").getID(), RuntimeRegressionExpressions$ELExpressions.ZONE_ID.getValue(context));
        assertEquals("UTF-8", RuntimeRegressionExpressions$ELExpressions.CHARSET_NAME.getValue(context));
    }

    @Test
    void aBooleanIsComparedWithAStringAsAString() {
        CompiledELContext context = new CompiledELContext();

        assertEquals(true, RuntimeRegressionExpressions$ELExpressions.BOOLEAN_LESS_THAN_STRING.getValue(context));
        assertEquals(true, RuntimeRegressionExpressions$ELExpressions.BOOLEAN_GREATER_THAN_STRING.getValue(context));
    }

    @Test
    void theUnaryMinusOfACharacterIsALong() {
        assertEquals((Object) (-49L),
            RuntimeRegressionExpressions$ELExpressions.NEGATED_CHARACTER.getValue(new CompiledELContext()));
    }

    @Test
    void theLengthOfAnArrayHasNoTypeToAssign() {
        CompiledELContext context = new CompiledELContext().setBean("arr", new int[]{1, 2, 3});

        assertNull(RuntimeRegressionExpressions$ELExpressions.ARRAY_LENGTH.getType(context));
        assertEquals((Object) 3, RuntimeRegressionExpressions$ELExpressions.ARRAY_LENGTH.getValue(context));
    }

    @Test
    void aCoercedLambdaIsSafeToCallConcurrently() {
        RegressionPlain bean = new RegressionPlain();
        CompiledELContext context = new CompiledELContext().setBean("bean", bean);

        for (int i = 0; i < 5; i++) {
            assertEquals(bean.big(), RuntimeRegressionExpressions$ELExpressions.PARALLEL_LAMBDA.getValue(context));
        }
    }

    @Test
    void anEscapingLambdaKeepsTheVariablesItWasCreatedWith() {
        ExpressionFactory factory = ExpressionFactory.newInstance();
        CompiledELContext creation = new CompiledELContext();
        creation.getVariableMapper().setVariable("s", factory.createValueExpression("str", String.class));
        CompiledELContext other = new CompiledELContext();
        other.getVariableMapper().setVariable("s", factory.createValueExpression("other", String.class));

        LambdaExpression lambda =
            (LambdaExpression) RuntimeRegressionExpressions$ELExpressions.ESCAPING_LAMBDA.getValue(creation);

        assertEquals("str2", lambda.invoke(other, 2L));
        assertEquals("str2", lambda.invoke(new CompiledELContext(), 2L));
    }

    /**
     * A superclass whose only bean is its subclass.
     */
    public static class RegressionBase {

        public String hello() {
            return "base";
        }
    }

    /**
     * The bean subclass, whose executable methods are not methods of its superclass instances.
     */
    @Singleton
    public static class RegressionSub extends RegressionBase {

        @Executable
        @Override
        public String hello() {
            return "sub";
        }

        @Executable
        public String subOnly() {
            return "subOnly";
        }

        @Executable
        public String fail() {
            throw new IllegalStateException("fail");
        }
    }

    /**
     * An introspected bean whose getter throws.
     */
    @Introspected
    public static class RegressionPerson {

        public String getBoom() {
            throw new IllegalStateException("boom");
        }
    }

    /**
     * A bean that is neither introspected nor a bean of the context, resolved reflectively.
     */
    public static class RegressionPlain {

        private int count;
        private String name = "plain";
        private TimeUnit unit = TimeUnit.MILLISECONDS;
        private final String[] sarr = new String[2];
        private final int[] ints = new int[2];

        public int getCount() {
            return count;
        }

        public void setCount(int count) {
            this.count = count;
        }

        public String getName() {
            return name;
        }

        public void setName(String name) {
            this.name = name;
        }

        public TimeUnit getUnit() {
            return unit;
        }

        public void setUnit(TimeUnit unit) {
            this.unit = unit;
        }

        public String[] getSarr() {
            return sarr;
        }

        public int[] getInts() {
            return ints;
        }

        public List<Long> big() {
            return LongStream.range(0, 200_000).boxed().collect(Collectors.toList());
        }
    }

    /**
     * Values of the platform whose runtime classes are implementation classes of a module.
     */
    public static class RegressionPojo {

        public TimeZone getZone() {
            return TimeZone.getTimeZone("UTC");
        }

        public Charset getCs() {
            return StandardCharsets.UTF_8;
        }
    }

    /**
     * A type with one real overload of a name the contributor registers another overload of.
     */
    public static class RegressionMoney {

        private final long cents;

        public RegressionMoney(long cents) {
            this.cents = cents;
        }

        public long getCents() {
            return cents;
        }

        public String two(int a, int b) {
            return "real-two";
        }
    }

    /**
     * Registers a method the type does not declare, and an overload of one it does.
     */
    public static class RegressionMoneyContributor implements ELMethodContributor {

        @Override
        public void contribute(ELMethodRegistry registry) {
            registry.method(RegressionMoney.class, "plus", Long.class, Long.class,
                (money, amount) -> money.getCents() + amount);
            registry.method(RegressionMoney.class, "two", String.class, String.class,
                (money, value) -> "contributed-two");
        }
    }
}
