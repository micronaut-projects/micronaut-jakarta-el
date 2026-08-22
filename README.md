# Micronaut Expression Language

An implementation of the [Jakarta Expression Language 6.0](https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0)
specification whose expressions and resolvers are generated at **compilation time** with
[Micronaut SourceGen](https://micronaut-projects.github.io/micronaut-sourcegen/latest/guide/) and an annotation
processor.

Nothing is parsed and nothing is resolved reflectively at runtime:

* every declared expression becomes a generated `jakarta.el.ValueExpression` or `jakarta.el.MethodExpression`
  implementation whose body is the compiled form of the expression;
* every bean becomes a generated `ELBeanResolver`, which replaces the reflective lookups of
  `jakarta.el.BeanELResolver`;
* every property access, method invocation, function call and static reference whose type is known at compilation
  time becomes a direct Java invocation.

## Modules

| Module                                      | Description                                                                             |
|---------------------------------------------|-----------------------------------------------------------------------------------------|
| `micronaut-expression-language-annotations` | The annotations used to declare beans, expressions and their environment                 |
| `micronaut-expression-language`             | The runtime: the resolvers, the coercion rules and the compiled expression base classes  |
| `micronaut-expression-language-parser`      | The lexer, the parser and the abstract syntax tree, with no dependency on the code generator |
| `micronaut-expression-language-processor`   | The annotation processor: the compiler and the writers                                   |
| `micronaut-expression-language-interpreter` | Optional. Parses and evaluates at runtime the expressions that were not compiled          |

The parser is a module of its own because the compiler is not its only consumer: the interpreter uses the same
abstract syntax tree, and so can any code that needs to inspect an expression without generating one.

## Declaring expressions

```java
@ELEnvironment(
    variables = @ELVariable(name = "book", type = Book.class),
    imports = Suit.class,
    functions = @ELFunctions(prefix = "fn", value = TextFunctions.class)
)
@ELExpression(value = "${book.title}", expectedType = String.class, name = "title")
@ELExpression(value = "Book: ${book.title} costs ${book.unitPrice}", expectedType = String.class, name = "summary")
@ELExpression(value = "${book.discounted(10)}", expectedType = Double.class, name = "discounted")
public final class BookExpressions {
}
```

The processor generates a `BookExpressions$ELExpressions` class holding one constant per expression, and one class
per expression. `${book.title}` compiles to:

```java
public final class BookExpressions$Expression0 extends CompiledValueExpression {

    public BookExpressions$Expression0() {
        super("${book.title}", java.lang.String.class);
    }

    @Override
    protected Object evaluate(ELContext context) {
        return ((Book) ELResolution.resolveIdentifier(context, "book")).getTitle();
    }

    @Override
    public void setValue(ELContext context, Object value) {
        ELResolution.setValue(context, (Book) ELResolution.resolveIdentifier(context, "book"), "title", value);
    }

    // getType, isReadOnly and getValueReference are generated for the lvalues too
}
```

The expressions are evaluated with any `jakarta.el.ELContext`:

```java
CompiledELContext context = new CompiledELContext().setBean("book", new Book("EL", "history", 20d));

String title = BookExpressions$ELExpressions.TITLE.getValue(context);
```

They are also returned by the `jakarta.el.ExpressionFactory` of the module, which is registered as a service, so
that the code depending on the standard API keeps working:

```java
ExpressionFactory factory = ExpressionFactory.newInstance();
ValueExpression expression = factory.createValueExpression(context, "${book.title}", String.class);
```

An expression that was not compiled and that is not a literal-expression is rejected, unless the interpreter
module is on the classpath. The lookup matches the expected type exactly: an expression declared with
`String.class` is only returned for a request with `String.class`.

Two behaviours of Micronaut's annotation metadata affect how an expression is declared. Any annotation string
containing `#{...}` is treated by Micronaut as one of its own evaluated expressions; the processor reads the
original text back out, but prefer `${...}`, which the specification parses identically and which Micronaut leaves
alone. And a primitive class literal such as `double.class` cannot be read from an annotation member, so
`expectedReturnType` and `expectedParamTypes` must use the wrapper types, which match the primitive declarations.

## Parsing at runtime

Compiling every expression is only possible when every expression is known at compilation time. When an expression
string is built at runtime, add the interpreter module:

```groovy
runtimeOnly("io.micronaut.el:micronaut-expression-language-interpreter")
```

It registers an `ELExpressionParser` service, which `CompiledExpressionFactory` consults for the expressions that no
generated source provides. Such an expression is parsed once, when it is created, and its tree is then evaluated by
`ELInterpreter`.

The interpreter is not a second implementation of the language: it walks the same abstract syntax tree the compiler
consumes and calls the same runtime as the generated code, so both share one definition of the semantics of the
specification. The compiled path remains the fast one, and the interpreted path is the fallback.

## Declaring beans

Beans are declared with Micronaut's own `@Introspected`, not with an annotation of this module:

```java
@Introspected
public class Book {

    public String getTitle() { ... }

    public void setTitle(String title) { ... }

    @Executable
    public double discounted(double percent) { ... }
}
```

Micronaut generates a `BeanIntrospection` for the type at compilation time, and `IntrospectionELResolver` reads
and writes the properties through it. The introspection carries a dispatch table of direct invocations, so no
reflection is involved. A method reaches the same path once it is annotated with `@Executable`, which is what puts
it in the introspection.

`IntrospectionELResolver` is the first resolver of the chain built by `ELResolvers.standard()`. A type with no
introspection is left unresolved, so the standard resolvers of the specification pick it up and a mixed model
still resolves.

Using `@Introspected` means any type that is already introspected for another reason — a Micronaut bean, a
`@Introspected(classes = ...)` declaration for a third party type — is resolvable by expressions with no further
annotation.

## When and how reflection is used

The module is built so that the paths a typical expression takes are reflection free, but it does not claim to
avoid reflection everywhere. Precisely:

**No reflection**

| Path                                                              | Mechanism                                              |
|-------------------------------------------------------------------|--------------------------------------------------------|
| A property or method of a variable whose type is declared with `@ELVariable` | Compiled to a direct Java invocation           |
| A property of an `@Introspected` type resolved dynamically         | The generated `BeanIntrospection` dispatch table        |
| A method of an `@Introspected` type annotated with `@Executable`    | The generated `BeanIntrospection` dispatch table        |
| A function declared with `@ELFunctions`                            | Compiled to a direct static invocation                  |
| An operator, a coercion, a collection operation, a lambda           | Compiled to a direct call into the runtime              |
| Locating a compiled expression by its string                       | A generated `switch`, no lookup and no parsing          |

**Reflection**

| Path                                                                   | Why                                                                 |
|-------------------------------------------------------------------------|---------------------------------------------------------------------|
| `MethodExpression` on a type that is not introspected, or a method that is not `@Executable` | The specification resolves the method against the base object at invocation time, so `ELMethods` selects it with `Class.getMethods()` and invokes it with `Method.invoke` |
| A function resolved at runtime through a `jakarta.el.FunctionMapper`     | The mapper's contract is `java.lang.reflect.Method`                  |
| `MethodExpression.getMethodInfo` and `getMethodReference`               | Both return reflective metadata by contract                          |
| A type with no `BeanIntrospection`, reached through the standard chain   | `jakarta.el.BeanELResolver` is reflective by design                  |
| Coercing a lambda expression to a functional interface (section 1.25.8)  | A `java.lang.reflect.Proxy` implements the interface                 |
| Coercing a string to a type with a `PropertyEditor` (section 1.25.9)     | `PropertyEditorManager` is the mechanism the specification names     |
| Reading and writing array elements and the `length` property            | `java.lang.reflect.Array`, which is how the JDK exposes arrays       |

The reflective paths are the ones the specification defines in reflective terms; they are not a fallback for
work that could have been generated. To keep a method invocation off them, annotate the method with
`@Executable` so that it enters the bean introspection.

## Language support

The annotation processor runs for Java, Groovy and Kotlin, and generates the same expressions for all three.

| Language | `@Introspected` beans | Expressions declared with `@ELExpression` | Generated as |
|----------|-----------------------|--------------------------------------------|--------------|
| Java     | Generated             | Generated                                   | Java source  |
| Groovy   | Generated             | Generated                                   | Bytecode     |
| Kotlin   | Generated             | Generated                                   | Bytecode     |

A Java build gets readable Java sources, which are worth reading when you want to see what an expression compiled
to. The Groovy and Kotlin source writers of Micronaut SourceGen cannot emit these classes yet, so those builds get
bytecode instead, written through `VisitorContext.visitClass` like every Micronaut bean definition. The result is
the same classes and the same behaviour; only the intermediate representation differs.

`test-suite-groovy` and `test-suite-kotlin` declare their expressions in Groovy and Kotlin source and assert that
they were compiled. Neither has the interpreter on its classpath, so an expression that reached the runtime
unparsed would fail rather than silently fall back.

## What is compiled statically

| Construct                                              | Compiled to                                        |
|--------------------------------------------------------|----------------------------------------------------|
| A property of a variable declared with `@ELVariable`     | The invocation of the getter                       |
| A method of a variable declared with `@ELVariable`       | The invocation of the method                       |
| A function declared with `@ELFunctions`                  | The invocation of the static method                |
| `ClassName.FIELD` of an imported class, an enum constant | The static field access                            |
| `ClassName.method(...)` of an imported class             | The invocation of the static method                |
| `ClassName(...)` of an imported class                    | The constructor invocation                         |
| `collection.stream()` of a known collection or array     | `ELStream.of(context, collection)`                 |
| A lambda expression                                      | A Java lambda holding the compiled body            |

Everything whose type is not known at compilation time is compiled to the resolution described in the sections 1.5
and 1.6 of the specification, which still goes through the generated resolvers when the type is annotated with
`@ELBean`.

## Specification coverage

* Chapter 1: the eval-expressions, the literal-expressions and the composite expressions, the full operator set with
  the precedence of the section 1.16, the identifiers, the functions, the variables, the lambda expressions, the
  enums, the arrays, the static field and method references, the constructor references and the type conversion rules
  of the section 1.25.
* Chapter 2: the construction of sets, lists and maps, and the 22 operations on collection objects, implemented by
  `ELStream` and `ELOptional`.

Functions are bound when the expression is created, as required by the section 1.18: the compiler binds them from
`@ELFunctions`, and the interpreter binds them from the `jakarta.el.FunctionMapper` of the context, so a later change
of the mapper does not affect an expression that already exists. The `jakarta.el.VariableMapper` and the
`jakarta.el.ImportHandler` of the context are consulted at evaluation time, as described in the sections 1.19
and 1.24.

## Technology Compatibility Kit

The `tests/jakarta-el-tck` module runs the Jakarta Expression Language 6.0 TCK against the runtime of this
repository:

```
./gradlew :micronaut-tests:micronaut-jakarta-el-tck:test
```

**360 tests, 0 failures, 0 skipped.**

The TCK bundle is not published to Maven Central, so it is resolved from the Eclipse download site as a plain
dependency, unpacked, and its test classes are handed to the test task. The version is set by `elTckBranch` and
`elTckVersion` in `gradle.properties`.

The TCK creates its expressions from strings at runtime through `ExpressionFactory.newInstance()`, so it exercises
the parser and the interpreter, and through them the coercion, arithmetic, comparison, resolution and collection
runtime that the generated code also calls. The signature test is excluded: it verifies the signatures of the
`jakarta.el` API jar, which this repository consumes unchanged rather than implements.

The TCK runs as part of `./gradlew build`.

## Building

```
./gradlew build
```

The build applies the Micronaut build conventions: Checkstyle, Spotless, Javadoc, the BOM and NullAway with
[JSpecify](https://jspecify.dev) annotations.
