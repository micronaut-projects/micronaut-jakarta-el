# Python Docs Disabled Test Inventory

This file tracks the Python documentation examples under `doc-examples/example-python` that are present but
disabled, or intentionally left out because the direct port of the Java example does not compile or does not
behave like the Java example yet. It is the bug-fixing task list for the Python compiler
(`micronaut-inject-python` / `micronaut-context-python`); every row references a `TODO(python)` comment in the
sources.

The Python examples are compiled by every build and their tests run with
`./gradlew pythonCheck -Ppython-ci` (the "Python CI" GitHub workflow).

## Migration Rules

- Do not define local copies of Micronaut annotation helpers or custom annotation shims in the examples. The
  annotations of the module are imported from their Java package (`micronaut.el.annotation`), the custom
  annotations of the `test-suite-custom-annotation` library from `micronaut.el.example.eligible`.
- Models are `@Introspected` dataclasses with idiomatic Python attributes; the attribute names are the property
  names the expressions use (`book.unitPrice`), so they keep the camelCase names of the Java examples.
- A Python test class is a `@MicronautTest` with injected beans; every test class runs in a GraalPy context of
  its own. Do not create nested `ApplicationContext.run(...)` contexts inside a Python test.
- Do not evaluate expressions, or initialise `jakarta.el.ELManager`, in the body of a class (at import time): the
  services the factory loads instantiate the Python `ELMethodContributor` on another thread, which imports the
  module holding it and waits for the import lock the importing thread holds. Create the factory in `__init__`
  or in the test method.
- Class-valued annotation members take Java types: `expectedType=str` / `float` map to `String` / `double`, a
  `List` is `java.util.List` (`list` does not resolve), `Math` is `java.lang.Math`.
- Java `Class` objects are compared by name (`getExpectedType().getName()`), not with `==`.
- Java classes are imported (`from java.lang import String, Double, Object, RuntimeException`,
  `from java.util import List`, `from micronaut.el import CompiledExpressionFactory, ELSandbox, ELBeanProvider`,
  `from micronaut.el.runtime import CompiledExpression`, `from micronaut.context import BeanDefinitionRegistry`);
  the imported form also works as a runtime type argument (`isinstance(x, CompiledExpression)`,
  `putContext(ELSandbox, ...)`, `putContext(PricingService, ...)`) and imported annotations as annotation metadata
  keys (`getAnnotation(MinAmount)`, `stringValue(Eligible)`). `java.type(...)` is used only where there is no import
  form:
  - the generated registries (`example.BookExpressions$ELExpressions`, ...): a name holding `$` cannot be imported
    (marked `TODO(python)`);
  - primitive and array class literals (`double`, `long`, `String[]`) in `BookMethods`;
  - the Java class generated for a Python exception (`java.type("example.NotEligibleException")`): once the
    exception raised by the interceptor has crossed into Java it is the generated Java class, which is what a Python
    caller of the intercepted method catches (as documented by the Python guide of Micronaut).
- A Python exception extends `RuntimeException`; the arguments of its `super().__init__(message)` call are forwarded
  to the Java constructor, so `getMessage()` is the message.
- `Summary`, the functional interface of the application, is an abstract class with one abstract method, which the
  Python compiler compiles to a Java interface; `functionalInterface(Summary, ...)` returns a Python class
  implementing it.

## Active `@Disabled` Tests

None.

## Commented Unsupported Snippet Ports

None.

## Workarounds In Place

| Where | Reason |
| --- | --- |
| `example.BookMethodsTest` builds its `ExpressionFactory` explicitly with the contributor | `ELContributions` loads the `META-INF/services/io.micronaut.el.ELMethodContributor` services once per JVM and keeps their registrations: the lambdas the Python `BookMethods` registers belong to the GraalPy context of the first test class that touched the factory, and are unusable (`jakarta.el.ELException: org.graalvm.polyglot.PolyglotException: Context execution was cancelled`) once that context is closed. An application runs one context, so the service registration works there; the registry would need a per-application rebuild for several contexts in one JVM. |
| `example.PricingExpressionsTest.ContextBeanProvider` | `ELBeanProvider` is a Java functional interface; a Python lambda cannot be handed to `ELContext.putContext(Class, Object)`, so a Python class implements the interface. |
