#!/usr/bin/env python3
"""Prints the result tables of EvaluationBenchmark from the JMH JSON results.

Usage: results.py <results.json> markdown|asciidoc|asciidoc-aggregate

Both tables end with the aggregate the docs report: the geometric mean of the average times of each stack over
all the benchmarks, and its ratio to the compiled stack.
"""
import sys

from chart import BENCHMARKS, STACKS, aggregate, load

LABELS = {
    "compiled": "Micronaut compiled",
    "interpreted": "Micronaut interpreted",
    "expressly": "Eclipse Expressly",
    "tomcat": "Tomcat Jasper EL",
}
EXPRESSIONS = {
    "property": "${book.title}",
    "nestedProperty": "${book.author.name}",
    "composite": "Book: ${book.title} costs ${book.unitPrice}",
    "arithmetic": "${book.unitPrice * 2 + 1 > 10 ? 'expensive' : 'cheap'}",
    "comparison": "${book.pages >= 300 and book.author.born < 1980}",
    "methodCall": "${book.discounted(10)}",
    "mapAccess": "${book.attributes['isbn']}",
    "stream": "${book.tags.stream().filter(t -> t.length() > 2).map(t -> t += '!').toList()}",
    "lambda": "${(x -> x * 2 + book.pages)(book.pages)}",
    "math": "${(book.unitPrice * book.pages - 100) / 3 mod 7 + -book.pages}",
    "complex": "${book.pages > 100 and book.unitPrice < 50 ? book.title += ' (' += book.author.name += ', ' += book.author.born += ')' : 'none'}",
    "listIndex": "${book.tags[1]}",
    "staticMethod": "${Integer.toHexString(book.pages)}",
    "stringMethods": "${book.title.toUpperCase().substring(0, 3) += '...'}",
    "emptyCheck": "${empty book.tags or book.tags.size() > 2}",
    "dynamicBean": "${order.customer.name}, a bean not declared with @ELVariable",
    "customLambda": "${book.adjusted((p, q) -> p * q + 1)}, a user functional interface",
    "createAndEvaluate": "create and evaluate the composite",
}


def guide_value(value):
    return f"{value:,.0f} ns" if value >= 100 else (f"{value:.1f} ns" if value >= 10 else f"{value:.2f} ns")


def markdown(scores):
    means = aggregate(scores)
    lines = ["| Benchmark | Stack | Score |", "| --- | --- | ---: |"]
    for name, _ in BENCHMARKS:
        for stack, _, _ in STACKS:
            score, error, unit = scores[(name, stack)]
            lines.append(f"| `{name}` | {LABELS[stack]} | {score:.3f} ± {error:.3f} {unit} |")
    lines.append("")
    lines.append("Aggregate, the geometric mean of the average times over all the benchmarks:")
    lines.append("")
    lines.append("| Stack | Geometric mean | Relative to compiled |")
    lines.append("| --- | ---: | ---: |")
    for stack, _, _ in STACKS:
        lines.append(f"| {LABELS[stack]} | {means[stack]:.2f} ns/op | {means[stack] / means['compiled']:.1f}x |")
    return "\n".join(lines)


def asciidoc_aggregate(scores):
    """The table of the user guide: the geometric means only."""
    means = aggregate(scores)
    lines = ['[cols="3,1,1"]', "|===", "|Implementation |Geometric mean of the average times |Relative to the compiled stack", ""]
    for stack, _, _ in STACKS:
        lines += ["|" + LABELS[stack], "|" + guide_value(means[stack]), f"|{means[stack] / means['compiled']:.1f}x", ""]
    lines.append("|===")
    return "\n".join(lines)


def asciidoc(scores):
    means = aggregate(scores)
    lines = ['[cols="4,1,1,1,1"]', "|===", "|Expression |" + " |".join(LABELS[stack] for stack, _, _ in STACKS), ""]
    for name, _ in BENCHMARKS:
        lines.append("|`" + EXPRESSIONS[name].replace("|", "\\|") + "`")
        for stack, _, _ in STACKS:
            lines.append("|" + guide_value(scores[(name, stack)][0]))
        lines.append("")
    lines.append("|*Geometric mean*")
    for stack, _, _ in STACKS:
        lines.append(f"|*{guide_value(means[stack])}*, {means[stack] / means['compiled']:.1f}x")
    lines.append("|===")
    return "\n".join(lines)


def main():
    scores = load(sys.argv[1])
    kind = sys.argv[2]
    print(markdown(scores) if kind == "markdown" else asciidoc_aggregate(scores) if kind == "asciidoc-aggregate" else asciidoc(scores))


if __name__ == "__main__":
    main()
