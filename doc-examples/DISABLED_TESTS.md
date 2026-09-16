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
- Only a method annotated `@Executable` (or the methods of a `@MicronautTest`) is bridged to the generated Java
  class, so a `@ELFunction` function, static or on a bean, also carries `@Executable`; without it the compiled
  expression fails at runtime with `NoSuchMethodError`.
- Java `Class` objects are compared by name (`getExpectedType().getName()`), not with `==`.
- Java classes are imported (`from java.lang import String, Double, Object, RuntimeException`,
  `from java.util import List`, `from micronaut.el import CompiledExpressionFactory`); the imported form also
  works as a `Class` argument for `java.lang` types and for the Python classes of the examples
  (`putContext(PricingService, ...)`). `java.type(...)` is used only where the import form fails, each use marked
  `TODO(python)`:
  - the generated registries (`example.BookExpressions$ELExpressions`, ...): a name holding `$` cannot be imported;
  - an imported Micronaut class as a runtime type argument: `isinstance(x, CompiledExpression)` is always false
    with the imported `micronaut.el.runtime.CompiledExpression`, and `putContext(ELSandbox, ...)`,
    `putContext(BeanDefinitionRegistry, ...)`, `putContext(ELBeanProvider, ...)` with the imported classes fail with
    `TypeError: invalid instantiation of foreign object`;
  - primitive and array class literals (`double`, `long`, `String[]`) in `BookMethods`, which have no import form;
  - the Java class generated for a Python exception (`java.type("example.NotEligibleException")`), see below.
- `java.type("io.micronaut.el.example.eligible.MinAmount")` is the decorator generated for the annotation, not its
  Java class, so annotation metadata is read by annotation name (`getAnnotation("io.micronaut...MinAmount")`,
  `stringValue("io.micronaut...Eligible")`); a decorator passed where a `Class` is expected fails with
  `TypeError: invalid instantiation of foreign object`.
- A Python exception extends `java.type("java.lang.RuntimeException")`; the class generated for it does not pass
  the message to `RuntimeException`, so the message is kept in a `message: str` attribute, which the generated
  class serves as `getMessage()`. Code catching it after it crossed Java (an intercepted call) catches the
  generated Java class (`java.type("example.NotEligibleException")`), not the Python class.

## Active `@Disabled` Tests

| Test | Reason |
| --- | --- |
| `example.BookMethodsTest.test_a_lambda_reaches_the_application_interface_without_a_proxy` | A Python class cannot declare a Java interface: the class generated for the Python `Summary` is a concrete class, so `ELMethodRegistry.functionalInterface(Summary, ...)` rejects it and the `summarised` method taking a `Summary` cannot be registered (callouts 6 and 10 of `BookMethods`). |

## Commented Unsupported Snippet Ports

| Snippet | Reason |
| --- | --- |
| `example.BookMethods` callouts 6 and 10 (`summarised`, `functionalInterface(Summary, ...)`) | See above; the two registrations are left out of the Python contributor with a `TODO(python)` comment. |

## Workarounds In Place

| Where | Reason |
| --- | --- |
| `example.BookMethodsTest` builds its `ExpressionFactory` explicitly with the contributor | `ELContributions` loads the `META-INF/services/io.micronaut.el.ELMethodContributor` services once per JVM; the Python `BookMethods` instance it creates belongs to the GraalPy context of the first test class that touched the factory, and is unusable ("Context execution was cancelled") once that context is closed. An application runs one context, so the service registration works there. |
| `example.PricingExpressionsTest.ContextBeanProvider` | `ELBeanProvider` is a Java functional interface; a Python lambda cannot be handed to `ELContext.putContext(Class, Object)`, so a Python class implements the interface. |
