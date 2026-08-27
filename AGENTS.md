# Repository Agent Guidelines

## Expression Test Parity

Every expression added to an interpreted regression test must have a matching compile-time-generated expression test, and every generated expression regression must have a matching interpreted test. Keep the expression text and expected result aligned so both execution modes exercise the same Jakarta EL behavior. For compile-time rejection tests that cannot produce a generated expression, add the closest equivalent interpreted runtime contract test and document why the generated case must fail compilation.
