<!-- Checklist: https://github.com/micronaut-projects/micronaut-core/wiki/New-Module-Checklist -->

# Micronaut Jakarta EL

[![Maven Central](https://img.shields.io/maven-central/v/io.micronaut.el/micronaut-jakarta-el.svg?label=Maven%20Central)](https://search.maven.org/search?q=g:%22io.micronaut.el%22%20AND%20a:%22micronaut-jakarta-el%22)
[![Build Status](https://github.com/micronaut-projects/micronaut-jakarta-el/workflows/Java%20CI/badge.svg)](https://github.com/micronaut-projects/micronaut-jakarta-el/actions)
[![Quality Gate Status](https://sonarcloud.io/api/project_badges/measure?project=micronaut-projects_micronaut-jakarta-el&metric=alert_status)](https://sonarcloud.io/summary/new_code?id=micronaut-projects_micronaut-jakarta-el)
[![Revved up by Develocity](https://img.shields.io/badge/Revved%20up%20by-Develocity-06A0CE?logo=Gradle&labelColor=02303A)](https://ge.micronaut.io/scans)

An implementation of the [Jakarta Expression Language 6.0](https://jakarta.ee/specifications/expression-language/6.0/jakarta-expression-language-spec-6.0)
specification whose expressions and resolvers are generated at **compilation time** with
[Micronaut SourceGen](https://micronaut-projects.github.io/micronaut-sourcegen/latest/guide/) and an annotation
processor.

The public API, annotations included, is marked `@Experimental`: it can change between minor versions until the first stable release.

Nothing is parsed and nothing is resolved reflectively at runtime:

* every declared expression becomes a generated `jakarta.el.ValueExpression` or `jakarta.el.MethodExpression`
  implementation whose body is the compiled form of the expression;
* every bean becomes a generated `ELBeanResolver`, which replaces the reflective lookups of
  `jakarta.el.BeanELResolver`;
* every property access, method invocation, function call and static reference whose type is known at compilation
  time becomes a direct Java invocation.

## Documentation

See the [Documentation](https://micronaut-projects.github.io/micronaut-jakarta-el/latest/guide/) for more information.

See the [Snapshot Documentation](https://micronaut-projects.github.io/micronaut-jakarta-el/snapshot/guide/) for the current development docs.

## Modules

| Module                                      | Description                                                                             |
|---------------------------------------------|-----------------------------------------------------------------------------------------|
| `micronaut-jakarta-el-annotations` | The annotations used to declare beans, expressions and their environment                 |
| `micronaut-jakarta-el`             | The runtime: the resolvers, the coercion rules and the compiled expression base classes  |
| `micronaut-jakarta-el-parser`      | The lexer, the parser and the abstract syntax tree, with no dependency on the code generator |
| `micronaut-jakarta-el-processor`   | The annotation processor: the compiler and the writers                                   |
| `micronaut-jakarta-el-interpreter` | Optional. Parses and evaluates runtime expressions through service-contributed executors |
| `micronaut-jakarta-el-interpreter-reflection` | Optional. Adds the reflection-backed executor for arbitrary Java methods and functions |

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
runtimeOnly("io.micronaut.el:micronaut-jakarta-el-interpreter")
// Optional: add reflective execution for methods not covered by a direct executor.
runtimeOnly("io.micronaut.el:micronaut-jakarta-el-interpreter-reflection")
```

It registers an `ELExpressionParser` service, which `CompiledExpressionFactory` consults for the expressions that no
generated source provides. Such an expression is parsed once, when it is created, and its tree is then evaluated by
`ELInterpreter`.

The interpreter is not a second implementation of the language: it walks the same abstract syntax tree the compiler
consumes and calls the same runtime as the generated code, so both share one definition of the semantics of the
specification. The interpreter module itself does not reflectively invoke Java methods. Its built-in service
contributors handle common String, collection, map, array, stream and optional operations, as well as Micronaut bean
introspections. Add `micronaut-jakarta-el-interpreter-reflection` when arbitrary public Java methods, constructors or
`FunctionMapper` methods must also be executable. The compiled path remains the fast one, and the interpreted path is
the fallback.
`CompiledVersusInterpretedTest` compiles expressions with the annotation processor and evaluates each of them
both ways, comparing the value, the type, the read-only flag, the value reference and the value after a write.

One difference between the two is not a defect. The compiler selects an overload from the **static** types of
the arguments, where the interpreter has only their runtime types: `${Math.max(book.pages, 1)}` compiles to
`Math.max(long, long)`, while at runtime an `Integer` and a `Long` match `max(int,int)`, `max(long,long)`,
`max(float,float)` and `max(double,double)` equally well and the reference is ambiguous. Both Expressly and
Tomcat Jasper EL report it ambiguous too. Declaring an expression therefore resolves overloads a runtime string
cannot.

## Expressions built from untrusted input

An expression declared with `@ELExpression` is source of the application. An expression string built at runtime
is not, and the specification resolves properties, methods, static members and constructors dynamically:
`${Runtime.getRuntime().exec(...)}` is a valid expression, and so is `${bean.getClass().getClassLoader()}`.
Adding the interpreter module to the classpath must not turn `ExpressionFactory.createValueExpression` into a
way to run arbitrary code.

Every expression the interpreter creates is therefore evaluated under an `ELSandbox`, which is consulted for the
base object of every property access and method invocation, for the class of every static reference and for the
class of every constructor reference. `ELSandbox.standard()`, the default, denies the types through which an
expression escapes into arbitrary Java:

| Denied                                                                                                              | Why                                                 |
|---------------------------------------------------------------------------------------------------------------------|-----------------------------------------------------|
| `Class`, `ClassLoader`, `Module`, `ModuleLayer`, `Package`, and every subtype                                        | The class of any object leads to every other class  |
| `Runtime`, `Process`, `ProcessBuilder`, `ProcessHandle`, `System`, `Thread`, `ThreadGroup`                           | The process itself                                  |
| `java.io.File`, `java.net.URI`, `java.net.URL`, `java.nio.file.Path`, `java.util.ServiceLoader`                      | The file system and the service loading             |
| `jakarta.el.ELContext`, `jakarta.el.ELResolver`                                                                      | An expression would otherwise widen its own sandbox |
| `java.lang.reflect`, `java.lang.invoke`, `java.lang.module`, `java.security`, `java.rmi`, `javax.naming`, `javax.script`, `jdk`, `sun` | Reflection and the platform internals |
| The members `class`, `getClass`, `getClassLoader`, `getModule`, `getProtectionDomain`, `wait`, `notify`, `notifyAll` | The step from an allowed object to a denied one     |

Everything else the language offers is untouched: the operators, the coercions, the collection operations, the
lambdas, and the properties and methods of the beans of the application. `com.sun` is deliberately **not**
denied: it is not reserved for the platform, and the TCK publishes its own beans under it. The TCK passes with
the sandbox in place.

An expression that reaches a denied type fails with an `ELSandboxException`. Compiled expressions do not go
through the sandbox at all. Register another one, `ELSandbox.UNRESTRICTED` included, on the context:

```java
context.putContext(ELSandbox.class, ELSandbox.UNRESTRICTED);
```

An expression only reaches a denied type through a bean of the application that exposes one, since the members
that lead to one from any object are denied. Reaching one is not the same as returning it, so the value an
expression hands back is checked too, as coerced to the expected type: `${bean.type}` requested as `Object`
fails, while requested as `String` it yields the coercion, through which nothing of the denied type escapes.
Only the value itself is examined; a denied object the application put inside a collection it exposes is not
searched for.

The sandbox bounds what an expression reaches, not what the beans it reaches then do, and an argument the
application's own method chose to accept is its own business. It keeps a runtime expression from escaping the
object graph it was given; it is not a licence to evaluate expressions written by an attacker.

The parser is bounded for the same reason. It is a recursive descent implementation and the tree it produces is
walked recursively, so an expression nested deeply enough would exhaust the call stack. An expression nested
more than `ELParser.DEFAULT_MAX_DEPTH` levels deep is rejected with an `ELParsingException`; parse with
`ELParser.parse(expression, maxDepth)` to raise the limit for an expression a tool generated.

### Deliberate divergences

Four behaviours differ from Expressly, from Tomcat Jasper EL, or from both. Each is a place the specification
leaves open, and the TCK passes either way, so this implementation keeps the reading that is the least
surprising:

| Behaviour | Here | Elsewhere |
|-----------|------|-----------|
| The right operand of a relational operator whose left operand is null | Evaluated, so `${null gt x}` reports that `x` cannot be resolved and `${null gt (y=1)}` performs the assignment | Both references skip it and return `false`. Only `&&`, `\|\|` and `?:` are specified to short-circuit |
| The iteration order of a set or map construction | Insertion order, so `${{'b','a'}}` is `[b, a]` and a map keeps the order its entries were written in | Both references use a hash set and a hash map, so the order is neither insertion nor sorted |
| The index of `${null[expr]}` | Evaluated | Expressly skips it; Tomcat rejects a null base outright |
| A backslash in literal text | `\'` stays `\'` and `\\` becomes `\` | Expressly drops every backslash; Tomcat keeps `\\` as `\\` |

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

The compiled runtime and the interpreter's built-in executors are reflection free for their direct paths. Reflection is
optional for runtime-parsed expressions: add `micronaut-jakarta-el-interpreter-reflection` when arbitrary public Java
members must be available.

**No reflection**

| Path                                                              | Mechanism                                              |
|-------------------------------------------------------------------|--------------------------------------------------------|
| A property or method of a variable whose type is declared with `@ELVariable` | Compiled to a direct Java invocation           |
| A property of an `@Introspected` type resolved dynamically         | The generated `BeanIntrospection` dispatch table        |
| A method of an `@Introspected` type annotated with `@Executable`    | The generated `BeanIntrospection` dispatch table        |
| A function declared with `@ELFunctions`                            | Compiled to a direct static invocation                  |
| An operator, a coercion, a collection operation, a lambda           | Compiled to a direct call into the runtime              |
| A String, collection, map, array, stream or optional method in an interpreted expression | A service-contributed direct executor |
| An interpreted method of an `@Introspected` type                    | The generated `BeanIntrospection` dispatch table        |
| Locating a compiled expression by its string                       | A generated `switch`, no lookup and no parsing          |

**Reflection**

| Path                                                                   | Why                                                                 |
|-------------------------------------------------------------------------|---------------------------------------------------------------------|
| `MethodExpression` on a type that is not introspected, or a method that is not `@Executable`, when the reflection companion is present | The reflection executor selects it with `ELMethods` and invokes it with `Method.invoke` |
| A function resolved at runtime through a `jakarta.el.FunctionMapper`, when the reflection companion is present     | The mapper's contract is `java.lang.reflect.Method`                  |
| Metadata for a reflection-backed method expression                               | The reflection executor reads the method's metadata                  |
| A type with no `BeanIntrospection`, reached through the standard chain   | `jakarta.el.BeanELResolver` is reflective by design                  |
| Coercing a lambda expression to a functional interface (section 1.25.8)  | A `java.lang.reflect.Proxy` implements the interface                 |
| Coercing a string to a type with a `PropertyEditor` (section 1.25.9)     | `PropertyEditorManager` is the mechanism the specification names     |
| A runtime-parsed method with no direct executor and no reflection companion | It fails with `MethodNotFoundException`; the interpreter has no reflective fallback |

To keep a method invocation off the reflective path, annotate the method with `@Executable` so that it enters the bean
introspection, or provide an `ELMethodExecutor` with a generated/direct implementation. The executor services are
ordered by priority, so direct contributors run before the general reflection fallback.

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
`@ELFunctions`, and the optional reflection executor binds them from the `jakarta.el.FunctionMapper` of the context,
so a later change of the mapper does not affect an expression that already exists. A generated executor can provide
the same binding without reflection. The `jakarta.el.VariableMapper` bindings are likewise captured when the
expression is created, as required by the section 1.19. Runtime-parsed expressions consult the
`jakarta.el.ImportHandler` during evaluation; generated expressions bind the imports declared by `@ELEnvironment`
at compilation time.

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

## Fuzzing

`ELFuzzTest` generates expressions from the grammar, mutates them and throws random strings at the parser and
the interpreter, asserting the invariants that hold for every input: the parser only ever fails with an
`ELParsingException`, the canonical form of an expression re-parses to itself, and an evaluation only ever
fails with an `ELException`. A failure is reduced to the shortest expression that still reproduces it before
it is reported, with the seed and the iteration that produced it.

The build runs 20 000 iterations. To run a longer campaign:

```
./gradlew :micronaut-jakarta-el-interpreter:test --tests '*ELFuzzTest*' -Dmicronaut.el.fuzz.iterations=1000000 -Dmicronaut.el.fuzz.seed=7
```

## Building

```
./gradlew build
```

The build applies the Micronaut build conventions: Checkstyle, Spotless, Javadoc, the BOM and NullAway with
[JSpecify](https://jspecify.dev) annotations.

## Snapshots and Releases

Snapshots are automatically published to [Sonatype Snapshots](https://central.sonatype.com/repository/maven-snapshots/io/micronaut/el/) using [GitHub Actions](https://github.com/micronaut-projects/micronaut-jakarta-el/actions).

See the documentation in the [Micronaut Docs](https://docs.micronaut.io/latest/guide/index.html#usingsnapshots) for how to configure your build to use snapshots.

Releases are published to Maven Central via [GitHub Actions](https://github.com/micronaut-projects/micronaut-jakarta-el/actions).

Releases are completely automated. To perform a release use the following steps:

* [Publish the draft release](https://github.com/micronaut-projects/micronaut-jakarta-el/releases). There should be already a draft release created, edit and publish it. The Git Tag should start with `v`. For example `v1.0.0`.
* [Monitor the Workflow](https://github.com/micronaut-projects/micronaut-jakarta-el/actions?query=workflow%3ARelease) to check it passed successfully.
* If everything went fine, [publish to Maven Central](https://github.com/micronaut-projects/micronaut-jakarta-el/actions?query=workflow%3A"Maven+Central+Sync").
* Celebrate!
