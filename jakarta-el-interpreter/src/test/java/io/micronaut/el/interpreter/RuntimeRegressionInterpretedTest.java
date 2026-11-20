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

import io.micronaut.context.ApplicationContext;
import io.micronaut.context.annotation.Executable;
import io.micronaut.core.annotation.Introspected;
import io.micronaut.core.io.service.SoftServiceLoader;
import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.ContributedELMethodExecutor;
import io.micronaut.el.ELMethodContributor;
import io.micronaut.el.ELMethodExecutor;
import io.micronaut.el.ELMethodRegistry;
import io.micronaut.el.ELSandbox;
import io.micronaut.el.ELSandboxException;
import jakarta.el.ELContext;
import jakarta.el.ELException;
import jakarta.el.ExpressionFactory;
import jakarta.el.MethodNotFoundException;
import jakarta.el.ValueExpression;
import jakarta.inject.Singleton;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

import java.nio.charset.Charset;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.LongStream;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The interpreted regressions of the runtime: the resolution, the coercion of an assignment, the sandbox and the
 * coercion of a lambda expression to a functional interface.
 *
 * <p>Every expression here has a compile-time generated counterpart, with the same text and the same expected
 * result, in {@code io.micronaut.el.test.RuntimeRegressionExpressions} of the Java test suite, with the exception
 * of the expressions the sandbox denies: an expression compiled at compilation time is source of the application
 * and never consults the sandbox, so its counterpart evaluates the expression where that is possible, and the
 * bare denied identifiers ({@code ${Class}} and the like), which name no value a compiled expression could
 * produce, have none. Neither has {@code ${String(FileInputStream(path).readAllBytes())}}, nor the beans decoder,
 * management and network expressions: the code the compiler generates for the first does not compile, since the
 * constructor and the method it calls declare checked exceptions, and the others would have to be run to be
 * compared.</p>
 */
class RuntimeRegressionInterpretedTest {

    private final ApplicationContext beanContext = ApplicationContext.run();

    private final ExpressionFactory factory =
        new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());

    @AfterEach
    void close() {
        beanContext.close();
    }

    // finding: a plain superclass instance is not dispatched through the definition of its bean subclass

    @Test
    void aSuperclassInstanceIsNotDispatchedAsItsBeanSubclass() {
        CompiledELContext context = new CompiledELContext(beanContext);
        context.setBean("base", new RegressionBase());
        context.setBean("sub", beanContext.getBean(RegressionSub.class));

        assertEquals("base", evaluate(context, "${base.hello()}"));
        assertEquals("sub", evaluate(context, "${sub.hello()}"));
        assertThrows(MethodNotFoundException.class, () -> evaluate(context, "${base.subOnly()}"));
    }

    // finding: an assignment coerces the value to the type of the property

    @Test
    void anAssignmentCoercesTheValueToTheTypeOfTheProperty() {
        CompiledELContext context = new CompiledELContext();
        RegressionPlain bean = new RegressionPlain();
        context.setBean("bean", bean);

        assertEquals((Object) 7, evaluate(context, "${bean.count = 7; bean.count}"));
        assertEquals((Object) 0, evaluate(context, "${bean.count = null; bean.count}"));
        assertEquals("5", evaluate(context, "${bean.name = 5; bean.name}"));
        assertEquals(TimeUnit.SECONDS, evaluate(context, "${bean.unit = 'SECONDS'; bean.unit}"));
        assertEquals("5", evaluate(context, "${bean.sarr[0] = 5; bean.sarr[0]}"));
        assertEquals((Object) 5, evaluate(context, "${bean.ints[0] = 5; bean.ints[0]}"));
        assertArrayEquals(new int[]{5, 0}, bean.getInts());
    }

    @Test
    void aFailedAssignmentIsAnELException() {
        CompiledELContext context = new CompiledELContext();
        context.setBean("bean", new RegressionPlain());

        ELException failure = assertThrows(ELException.class, () -> evaluate(context, "${bean.count = 'x'}"));
        assertFalse(failure instanceof ELSandboxException);
        assertThrows(ELException.class, () -> evaluate(context, "${bean.ints[0] = 'x'}"));
    }

    @Test
    void setValueCoercesTheValueToTheTypeOfTheProperty() {
        CompiledELContext context = new CompiledELContext();
        RegressionPlain bean = new RegressionPlain();
        context.setBean("bean", bean);

        factory.createValueExpression(context, "${bean.count}", Object.class).setValue(context, "5");

        assertEquals((Object) 5, bean.getCount());
    }

    // finding: a method of an Optional base

    @Test
    void aMethodOfAnOptionalIsTheMethodOfItsValue() {
        ExpressionFactory unreflected =
            new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(unreflectedExecutors()));
        for (ExpressionFactory each : List.of(factory, unreflected)) {
            CompiledELContext context = optionals();

            assertEquals((Object) 2, evaluate(each, context, "${optS.length()}"));
            assertNull(evaluate(each, context, "${optE.length()}"));
            assertEquals("plain", evaluate(each, context, "${optB.name}"));
            assertNull(evaluate(each, context, "${optE.name}"));
        }
    }

    // finding: a contributed method is found from a dynamically typed receiver, and does not shadow a real one

    @Test
    void aContributedMethodDoesNotShadowAnOverloadTheTypeDeclares() {
        List<ELMethodExecutor> executors = new ArrayList<>(
            SoftServiceLoader.load(ELMethodExecutor.class).collectAll());
        executors.removeIf(ContributedELMethodExecutor.class::isInstance);
        executors.add(new ContributedELMethodExecutor(List.of(new MoneyContributor())));
        ExpressionFactory contributed =
            new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser(executors));
        CompiledELContext context = new CompiledELContext();
        context.setBean("m", new Money(5));

        assertEquals((Object) 6L, evaluate(contributed, context, "${m.plus(1)}"));
        assertEquals("contributed-two", evaluate(contributed, context, "${m.two('x')}"));
        assertEquals("real-two", evaluate(contributed, context, "${m.two(1, 2)}"));
    }

    // finding: what a generated getter, setter or method throws is an ELException

    @Test
    void whatAGeneratedMemberThrowsIsAnELException() {
        CompiledELContext context = new CompiledELContext(beanContext);
        context.setBean("person", new RegressionPerson());
        context.setBean("sub", beanContext.getBean(RegressionSub.class));

        ELException getter = assertThrows(ELException.class, () -> evaluate(context, "${person.boom}"));
        assertInstanceOf(IllegalStateException.class, getter.getCause());
        ELException method = assertThrows(ELException.class, () -> evaluate(context, "${sub.fail()}"));
        assertInstanceOf(IllegalStateException.class, method.getCause());
    }

    // finding: the sandbox denies what an application import reaches beyond java.lang

    @Test
    void theSandboxDeniesTheFilesTheBeansDecoderAndTheProcess(@TempDir Path directory) throws Exception {
        Path secret = Files.writeString(directory.resolve("secret.txt"), "secret-content");
        Path marker = directory.resolve("secret.txt.pwned");
        String xml = "<java><object class=\"java.lang.ProcessBuilder\"><array class=\"java.lang.String\" length=\"2\">"
            + "<void index=\"0\"><string>touch</string></void><void index=\"1\"><string>" + marker
            + "</string></void></array><void method=\"start\"/></object></java>";
        CompiledELContext context = new CompiledELContext();
        for (String imported : List.of("java.io.FileInputStream", "java.io.File", "java.beans.XMLDecoder",
            "java.io.ByteArrayInputStream", "java.lang.management.ManagementFactory", "java.net.InetAddress")) {
            context.getImportHandler().importClass(imported);
        }
        context.setBean("path", secret.toString());
        context.setBean("xml", xml);

        assertDenied(context, "${String(FileInputStream(path).readAllBytes())}");
        assertDenied(context, "${File(path).exists()}");
        assertDenied(context, "${XMLDecoder(ByteArrayInputStream(xml.getBytes())).readObject()}");
        assertDenied(context, "${ManagementFactory.getPlatformMBeanServer()}");
        assertDenied(context, "${InetAddress.getLocalHost()}");
        assertDenied(context, "${StackWalker.getInstance()}");
        assertFalse(Files.exists(marker), "the process must not have been started");

        // the counterparts the generated expressions evaluate, which an expression parsed at runtime reaches only
        // once the application lifted the sandbox
        context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        assertEquals("secret-content", evaluate(context, "${String(FileInputStream(path).readAllBytes())}"));
        assertEquals((Object) true, evaluate(context, "${File(path).exists()}"));
    }

    @Test
    void theSandboxDeniesTheSystemPropertyReaders() {
        CompiledELContext context = new CompiledELContext();

        assertDenied(context, "${Boolean.getBoolean('micronaut.el.regression')}");
        assertDenied(context, "${Integer.getInteger('micronaut.el.regression')}");
        assertDenied(context, "${Long.getLong('micronaut.el.regression')}");

        CompiledELContext unrestricted = new CompiledELContext();
        unrestricted.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
        assertEquals((Object) false, evaluate(unrestricted, "${Boolean.getBoolean('micronaut.el.regression')}"));
        assertNull(evaluate(unrestricted, "${Integer.getInteger('micronaut.el.regression')}"));
        assertNull(evaluate(unrestricted, "${Long.getLong('micronaut.el.regression')}"));
    }

    @Test
    void aDeniedClassIsNotAValueOfAnIdentifier() {
        // no generated counterpart: a compiled expression is never sandboxed, and a bare class is no value of it
        CompiledELContext context = new CompiledELContext();

        assertDenied(context, "${Class}");
        assertDenied(context, "${Thread}");
        assertDenied(context, "${Module}");
        assertDenied(context, "${[Thread]}");
    }

    // finding: the sandbox judges a value of the platform by its public type

    @Test
    void theSandboxJudgesAValueByItsPublicType() {
        CompiledELContext context = new CompiledELContext();
        context.setBean("pojo", new RegressionPojo());

        assertEquals(TimeZone.getTimeZone("UTC").getID(), evaluate(context, "${pojo.zone.ID}"));
        assertEquals("UTF-8", evaluate(context, "${pojo.cs.name()}"));
    }

    // findings: the specification nits

    @Test
    void aBooleanIsComparedWithAStringAsAString() {
        CompiledELContext context = new CompiledELContext();

        assertEquals((Object) true, evaluate(context, "${true lt 'zzz'}"));
        assertEquals((Object) true, evaluate(context, "${false gt '9'}"));
    }

    @Test
    void theUnaryMinusOfACharacterIsALong() {
        assertEquals((Object) (-49L), evaluate(new CompiledELContext(), "${-Character.valueOf(49)}"));
    }

    @Test
    void theLengthOfAnArrayHasNoTypeToAssign() {
        CompiledELContext context = new CompiledELContext();
        context.setBean("arr", new int[]{1, 2, 3});

        ValueExpression length = factory.createValueExpression(context, "${arr.length}", Object.class);

        assertNull(length.getType(context));
        assertEquals((Object) 3, length.getValue(context));
    }

    // finding: a lambda expression coerced to a functional interface is safe to call from several threads

    @Test
    void aCoercedLambdaIsSafeToCallConcurrently() {
        CompiledELContext context = new CompiledELContext();
        RegressionPlain bean = new RegressionPlain();
        context.setBean("bean", bean);

        for (int i = 0; i < 5; i++) {
            assertEquals(bean.big(), evaluate(context, "${bean.big().parallelStream().map(x -> x + 0).toList()}"));
        }
    }

    private Object evaluate(ELContext context, String expression) {
        return evaluate(factory, context, expression);
    }

    private static Object evaluate(ExpressionFactory factory, ELContext context, String expression) {
        return factory.createValueExpression(context, expression, Object.class).getValue(context);
    }

    private void assertDenied(ELContext context, String expression) {
        assertThrows(ELSandboxException.class, () -> evaluate(context, expression), expression);
    }

    private static CompiledELContext optionals() {
        CompiledELContext context = new CompiledELContext();
        context.setBean("optS", Optional.of("ab"));
        context.setBean("optE", Optional.empty());
        context.setBean("optB", Optional.of(new RegressionPlain()));
        return context;
    }

    private static List<ELMethodExecutor> unreflectedExecutors() {
        List<ELMethodExecutor> executors = new ArrayList<>(
            SoftServiceLoader.load(ELMethodExecutor.class).collectAll());
        executors.removeIf(ELMethodExecutor::isReflective);
        return executors;
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
    public static class Money {

        private final long cents;

        public Money(long cents) {
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
    public static class MoneyContributor implements ELMethodContributor {

        @Override
        public void contribute(ELMethodRegistry registry) {
            registry.method(Money.class, "plus", Long.class, Long.class, (money, amount) -> money.getCents() + amount);
            registry.method(Money.class, "two", String.class, String.class, (money, value) -> "contributed-two");
        }
    }
}
