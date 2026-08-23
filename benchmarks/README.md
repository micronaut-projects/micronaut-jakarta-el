# Benchmarks

JMH microbenchmarks of the evaluation of Jakarta Expression Language expressions, comparing:

- `compiled` — Micronaut Jakarta EL, the expressions compiled by the annotation processor;
- `interpreted` — Micronaut Jakarta EL, the same expressions parsed at runtime by `micronaut-jakarta-el-interpreter`;
- `expressly` — [Eclipse Expressly](https://github.com/eclipse-ee4j/expressly), the reference implementation of the specification;
- `tomcat` — [Apache Tomcat Jasper EL](https://tomcat.apache.org/).

`EvaluationBenchmark` evaluates a `jakarta.el.ValueExpression` created once, against a context holding the
`book` bean under its name with the default resolvers of each implementation. `createAndEvaluate` creates the
expression from its string on every invocation: a registry lookup for the compiled stack, a parse or a parse
cache lookup for the others.

## Running

```bash
./gradlew :micronaut-benchmarks:jmh
```

Overrides: `-Pjmh.includes=<regex>`, `-Pjmh.forks=N`, `-Pjmh.warmupIterations=N`, `-Pjmh.iterations=N`,
`-Pjmh.param.stack=compiled,expressly`, `-Pjmh.profilers=async:...`, `-Pjmh.jvmArgs=...`. The results are
written to `build/results/jmh/results.json`; the chart is rendered from them:

```bash
python3 benchmarks/chart.py benchmarks/build/results/jmh/results.json benchmarks/evaluation-benchmark-results.svg "subtitle"
cp benchmarks/evaluation-benchmark-results.svg src/main/docs/resources/img/
```

When refreshing the results, regenerate the table below, the table of `src/main/docs/guide/whyCompiled.adoc` (and its summary paragraph, updated by hand) and
both copies of the chart with `benchmarks/refresh.sh "<environment and settings>"`.

## Latest local results

<!-- results:start -->
OpenJDK 25.0.2 on Apple Silicon, JMH 1.37, 1 fork, 3 warmup and 5 measurement iterations of 1 s, average time.

| Benchmark | Stack | Score |
| --- | --- | ---: |
| `property` | Micronaut compiled | 4.590 ± 0.038 ns/op |
| `property` | Micronaut interpreted | 15.404 ± 0.577 ns/op |
| `property` | Eclipse Expressly | 100.946 ± 2.221 ns/op |
| `property` | Tomcat Jasper EL | 119.098 ± 11.704 ns/op |
| `nestedProperty` | Micronaut compiled | 4.669 ± 0.085 ns/op |
| `nestedProperty` | Micronaut interpreted | 32.009 ± 1.268 ns/op |
| `nestedProperty` | Eclipse Expressly | 156.750 ± 2.996 ns/op |
| `nestedProperty` | Tomcat Jasper EL | 170.011 ± 5.007 ns/op |
| `composite` | Micronaut compiled | 27.912 ± 0.391 ns/op |
| `composite` | Micronaut interpreted | 189.516 ± 6.418 ns/op |
| `composite` | Eclipse Expressly | 303.469 ± 15.580 ns/op |
| `composite` | Tomcat Jasper EL | 424.037 ± 19.259 ns/op |
| `arithmetic` | Micronaut compiled | 3.347 ± 0.122 ns/op |
| `arithmetic` | Micronaut interpreted | 26.794 ± 0.793 ns/op |
| `arithmetic` | Eclipse Expressly | 113.264 ± 1.470 ns/op |
| `arithmetic` | Tomcat Jasper EL | 271.006 ± 11.974 ns/op |
| `comparison` | Micronaut compiled | 3.519 ± 0.070 ns/op |
| `comparison` | Micronaut interpreted | 64.429 ± 1.858 ns/op |
| `comparison` | Eclipse Expressly | 237.788 ± 11.808 ns/op |
| `comparison` | Tomcat Jasper EL | 529.317 ± 21.757 ns/op |
| `methodCall` | Micronaut compiled | 3.288 ± 0.041 ns/op |
| `methodCall` | Micronaut interpreted | 40.358 ± 1.886 ns/op |
| `methodCall` | Eclipse Expressly | 300.276 ± 8.141 ns/op |
| `methodCall` | Tomcat Jasper EL | 323.753 ± 14.751 ns/op |
| `mapAccess` | Micronaut compiled | 4.993 ± 0.052 ns/op |
| `mapAccess` | Micronaut interpreted | 40.424 ± 0.852 ns/op |
| `mapAccess` | Eclipse Expressly | 118.975 ± 3.708 ns/op |
| `mapAccess` | Tomcat Jasper EL | 125.734 ± 3.297 ns/op |
| `stream` | Micronaut compiled | 45.461 ± 1.784 ns/op |
| `stream` | Micronaut interpreted | 487.100 ± 7.153 ns/op |
| `stream` | Eclipse Expressly | 2842.416 ± 14.245 ns/op |
| `stream` | Tomcat Jasper EL | 3419.888 ± 58.279 ns/op |
| `lambda` | Micronaut compiled | 5.678 ± 0.132 ns/op |
| `lambda` | Micronaut interpreted | 119.355 ± 1.215 ns/op |
| `lambda` | Eclipse Expressly | 303.090 ± 16.217 ns/op |
| `lambda` | Tomcat Jasper EL | 356.624 ± 11.858 ns/op |
| `math` | Micronaut compiled | 9.244 ± 0.331 ns/op |
| `math` | Micronaut interpreted | 71.261 ± 1.657 ns/op |
| `math` | Eclipse Expressly | 277.307 ± 11.344 ns/op |
| `math` | Tomcat Jasper EL | 297.220 ± 7.156 ns/op |
| `complex` | Micronaut compiled | 15.680 ± 1.385 ns/op |
| `complex` | Micronaut interpreted | 183.431 ± 3.003 ns/op |
| `complex` | Eclipse Expressly | 654.960 ± 19.882 ns/op |
| `complex` | Tomcat Jasper EL | 1237.356 ± 15.380 ns/op |
| `listIndex` | Micronaut compiled | 5.029 ± 0.094 ns/op |
| `listIndex` | Micronaut interpreted | 45.407 ± 1.676 ns/op |
| `listIndex` | Eclipse Expressly | 125.805 ± 4.224 ns/op |
| `listIndex` | Tomcat Jasper EL | 134.242 ± 4.304 ns/op |
| `staticMethod` | Micronaut compiled | 9.530 ± 0.617 ns/op |
| `staticMethod` | Micronaut interpreted | 146.020 ± 5.296 ns/op |
| `staticMethod` | Eclipse Expressly | 2449.432 ± 25.723 ns/op |
| `staticMethod` | Tomcat Jasper EL | 1577.350 ± 58.676 ns/op |
| `stringMethods` | Micronaut compiled | 34.659 ± 7.213 ns/op |
| `stringMethods` | Micronaut interpreted | 253.005 ± 24.161 ns/op |
| `stringMethods` | Eclipse Expressly | 1807.780 ± 90.820 ns/op |
| `stringMethods` | Tomcat Jasper EL | 1898.692 ± 95.458 ns/op |
| `emptyCheck` | Micronaut compiled | 3.519 ± 0.052 ns/op |
| `emptyCheck` | Micronaut interpreted | 95.365 ± 79.382 ns/op |
| `emptyCheck` | Eclipse Expressly | 1460.221 ± 64.732 ns/op |
| `emptyCheck` | Tomcat Jasper EL | 1662.283 ± 52.741 ns/op |
| `dynamicBean` | Micronaut compiled | 18.932 ± 0.610 ns/op |
| `dynamicBean` | Micronaut interpreted | 31.415 ± 1.387 ns/op |
| `dynamicBean` | Eclipse Expressly | 158.525 ± 4.198 ns/op |
| `dynamicBean` | Tomcat Jasper EL | 166.585 ± 4.028 ns/op |
| `customLambda` | Micronaut compiled | 4.319 ± 0.052 ns/op |
| `customLambda` | Micronaut interpreted | 162.818 ± 2.947 ns/op |
| `customLambda` | Eclipse Expressly | 463.683 ± 15.708 ns/op |
| `customLambda` | Tomcat Jasper EL | 471.047 ± 16.855 ns/op |
| `createAndEvaluate` | Micronaut compiled | 34.361 ± 3.125 ns/op |
| `createAndEvaluate` | Micronaut interpreted | 208.820 ± 2.905 ns/op |
| `createAndEvaluate` | Eclipse Expressly | 340.102 ± 3.407 ns/op |
| `createAndEvaluate` | Tomcat Jasper EL | 461.123 ± 124.924 ns/op |

Aggregate, the geometric mean of the average times over all the benchmarks:

| Stack | Geometric mean | Relative to compiled |
| --- | ---: | ---: |
| Micronaut compiled | 8.63 ns/op | 1.0x |
| Micronaut interpreted | 82.90 ns/op | 9.6x |
| Eclipse Expressly | 367.16 ns/op | 42.5x |
| Tomcat Jasper EL | 448.71 ns/op | 52.0x |

![EvaluationBenchmark local results](evaluation-benchmark-results.svg)
<!-- results:end -->
