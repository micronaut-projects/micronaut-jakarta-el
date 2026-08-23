package io.micronaut.el.benchmark;

import io.micronaut.el.CompiledELContext;
import io.micronaut.el.CompiledExpressionFactory;
import io.micronaut.el.interpreter.InterpretingELExpressionParser;
import jakarta.el.ELContext;
import jakarta.el.ELManager;
import jakarta.el.ExpressionFactory;
import jakarta.el.ValueExpression;
import org.openjdk.jmh.annotations.Benchmark;
import org.openjdk.jmh.annotations.BenchmarkMode;
import org.openjdk.jmh.annotations.Mode;
import org.openjdk.jmh.annotations.OutputTimeUnit;
import org.openjdk.jmh.annotations.Param;
import org.openjdk.jmh.annotations.Scope;
import org.openjdk.jmh.annotations.Setup;
import org.openjdk.jmh.annotations.State;

import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

/**
 * Evaluates the same expressions with the compiled implementation, with this module's interpreter and with the
 * reference and the Tomcat implementations of the specification.
 *
 * <p>Every stack evaluates a {@code jakarta.el.ValueExpression} created once, against a context holding the
 * {@code book} bean under its name, with the resolvers the implementation provides by default. The
 * {@code createAndEvaluate} benchmark creates the expression from its string on every invocation: a registry
 * lookup for the compiled stack, a parse, or a parse cache lookup, for the others.</p>
 */
@State(Scope.Benchmark)
@BenchmarkMode(Mode.AverageTime)
@OutputTimeUnit(TimeUnit.NANOSECONDS)
public class EvaluationBenchmark {

    static final String PROPERTY = "${book.title}";
    static final String NESTED_PROPERTY = "${book.author.name}";
    static final String COMPOSITE = "Book: ${book.title} costs ${book.unitPrice}";
    static final String ARITHMETIC = "${book.unitPrice * 2 + 1 > 10 ? 'expensive' : 'cheap'}";
    static final String COMPARISON = "${book.pages >= 300 and book.author.born < 1980}";
    static final String METHOD_CALL = "${book.discounted(10)}";
    static final String MAP_ACCESS = "${book.attributes['isbn']}";
    static final String STREAM = "${book.tags.stream().filter(t -> t.length() > 2).map(t -> t += '!').toList()}";
    static final String LAMBDA = "${(x -> x * 2 + book.pages)(book.pages)}";
    static final String MATH = "${(book.unitPrice * book.pages - 100) / 3 mod 7 + -book.pages}";
    static final String COMPLEX = "${book.pages > 100 and book.unitPrice < 50 ? book.title += ' (' += book.author.name += ', ' += book.author.born += ')' : 'none'}";
    static final String LIST_INDEX = "${book.tags[1]}";
    static final String STATIC_METHOD = "${Integer.toHexString(book.pages)}";
    static final String STRING_METHODS = "${book.title.toUpperCase().substring(0, 3) += '...'}";
    static final String EMPTY_CHECK = "${empty book.tags or book.tags.size() > 2}";
    static final String DYNAMIC_BEAN = "${order.customer.name}";
    static final String CUSTOM_LAMBDA = "${book.adjusted((p, q) -> p * q + 1)}";

    /**
     * The implementation: {@code compiled} is this module's annotation processor, {@code interpreted} this
     * module's runtime parser, {@code expressly} the reference implementation and {@code tomcat} Tomcat's.
     */
    @Param({"compiled", "interpreted", "expressly", "tomcat"})
    public String stack;

    private ELContext context;
    private ExpressionFactory factory;
    private ValueExpression property;
    private ValueExpression nestedProperty;
    private ValueExpression composite;
    private ValueExpression arithmetic;
    private ValueExpression comparison;
    private ValueExpression methodCall;
    private ValueExpression mapAccess;
    private ValueExpression stream;
    private ValueExpression lambda;
    private ValueExpression math;
    private ValueExpression complex;
    private ValueExpression listIndex;
    private ValueExpression staticMethod;
    private ValueExpression stringMethods;
    private ValueExpression emptyCheck;
    private ValueExpression dynamicBean;
    private ValueExpression customLambda;

    @Setup
    public void setup() {
        Book book = new Book("Expression Language", 20d, 320,
            new Author("Jakarta", 1975), List.of("new", "sale", "b"), Map.of("isbn", "978-0"));
        // a bean the expressions do not declare: resolved by the resolvers at runtime on every stack
        Order order = new Order(new Author("Customer", 1980));
        switch (stack) {
            case "compiled" -> {
                factory = new CompiledExpressionFactory();
                context = new CompiledELContext().setBean("book", book).setBean("order", order);
            }
            case "interpreted" -> {
                factory = new CompiledExpressionFactory(List.of(), new InterpretingELExpressionParser());
                context = new CompiledELContext().setBean("book", book).setBean("order", order);
            }
            case "expressly" -> {
                factory = new org.glassfish.expressly.ExpressionFactoryImpl();
                context = standardContext(factory, book, order);
            }
            case "tomcat" -> {
                factory = new org.apache.el.ExpressionFactoryImpl();
                context = standardContext(factory, book, order);
            }
            default -> throw new IllegalArgumentException(stack);
        }
        property = factory.createValueExpression(context, PROPERTY, String.class);
        nestedProperty = factory.createValueExpression(context, NESTED_PROPERTY, String.class);
        composite = factory.createValueExpression(context, COMPOSITE, String.class);
        arithmetic = factory.createValueExpression(context, ARITHMETIC, String.class);
        comparison = factory.createValueExpression(context, COMPARISON, Boolean.class);
        methodCall = factory.createValueExpression(context, METHOD_CALL, Double.class);
        mapAccess = factory.createValueExpression(context, MAP_ACCESS, String.class);
        stream = factory.createValueExpression(context, STREAM, Object.class);
        lambda = factory.createValueExpression(context, LAMBDA, Object.class);
        math = factory.createValueExpression(context, MATH, Object.class);
        complex = factory.createValueExpression(context, COMPLEX, String.class);
        listIndex = factory.createValueExpression(context, LIST_INDEX, String.class);
        staticMethod = factory.createValueExpression(context, STATIC_METHOD, String.class);
        stringMethods = factory.createValueExpression(context, STRING_METHODS, String.class);
        emptyCheck = factory.createValueExpression(context, EMPTY_CHECK, Boolean.class);
        dynamicBean = factory.createValueExpression(context, DYNAMIC_BEAN, String.class);
        customLambda = factory.createValueExpression(context, CUSTOM_LAMBDA, Double.class);
        verify();
    }

    /**
     * The standard context of the specification, with the bean defined under its name through the
     * {@link ELManager}, for the implementations that provide no context of their own.
     */
    private static ELContext standardContext(ExpressionFactory factory, Book book, Order order) {
        // the manager creates the context with the factory the system property names
        System.setProperty("jakarta.el.ExpressionFactory", factory.getClass().getName());
        ELManager manager = new ELManager();
        manager.defineBean("book", book);
        manager.defineBean("order", order);
        return manager.getELContext();
    }

    private void verify() {
        expect("Expression Language", property.getValue(context));
        expect("Jakarta", nestedProperty.getValue(context));
        expect("Book: Expression Language costs 20.0", composite.getValue(context));
        expect("expensive", arithmetic.getValue(context));
        expect(Boolean.TRUE, comparison.getValue(context));
        expect(18d, methodCall.getValue(context));
        expect("978-0", mapAccess.getValue(context));
        expect(List.of("new!", "sale!"), stream.getValue(context));
        expect(960L, lambda.getValue(context));
        expect((20d * 320 - 100) / 3 % 7 + -320, math.getValue(context));
        expect("Expression Language (Jakarta, 1975)", complex.getValue(context));
        expect("sale", listIndex.getValue(context));
        expect("140", staticMethod.getValue(context));
        expect("EXP...", stringMethods.getValue(context));
        expect(Boolean.TRUE, emptyCheck.getValue(context));
        expect("Customer", dynamicBean.getValue(context));
        expect(20d * 320 + 1, customLambda.getValue(context));
    }

    private static void expect(Object expected, Object actual) {
        if (!expected.equals(actual)) {
            throw new IllegalStateException("Expected " + expected + " but was " + actual);
        }
    }

    @Benchmark
    public Object property() {
        return property.getValue(context);
    }

    @Benchmark
    public Object nestedProperty() {
        return nestedProperty.getValue(context);
    }

    @Benchmark
    public Object composite() {
        return composite.getValue(context);
    }

    @Benchmark
    public Object arithmetic() {
        return arithmetic.getValue(context);
    }

    @Benchmark
    public Object comparison() {
        return comparison.getValue(context);
    }

    @Benchmark
    public Object methodCall() {
        return methodCall.getValue(context);
    }

    @Benchmark
    public Object mapAccess() {
        return mapAccess.getValue(context);
    }

    @Benchmark
    public Object stream() {
        return stream.getValue(context);
    }

    @Benchmark
    public Object lambda() {
        return lambda.getValue(context);
    }

    @Benchmark
    public Object math() {
        return math.getValue(context);
    }

    @Benchmark
    public Object complex() {
        return complex.getValue(context);
    }

    @Benchmark
    public Object listIndex() {
        return listIndex.getValue(context);
    }

    @Benchmark
    public Object staticMethod() {
        return staticMethod.getValue(context);
    }

    @Benchmark
    public Object stringMethods() {
        return stringMethods.getValue(context);
    }

    @Benchmark
    public Object emptyCheck() {
        return emptyCheck.getValue(context);
    }

    @Benchmark
    public Object dynamicBean() {
        return dynamicBean.getValue(context);
    }

    @Benchmark
    public Object customLambda() {
        return customLambda.getValue(context);
    }

    @Benchmark
    public Object createAndEvaluate() {
        return factory.createValueExpression(context, COMPOSITE, String.class).getValue(context);
    }
}
