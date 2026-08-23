#!/usr/bin/env python3
"""Renders the JMH results of EvaluationBenchmark as a static SVG bar chart.

Usage: chart.py <results.json> <out.svg> [subtitle] [--aggregate]

One panel per benchmark, preceded by the aggregate, one bar per stack, an x-axis per panel starting at zero
with a rounded maximum, ns/op (lower is better). With --aggregate only the aggregate panel is rendered, the
compact chart of the user guide. No external fonts, scripts or images.
"""
import json
import math
import sys

STACKS = [
    ("compiled", "Micronaut Jakarta EL, compiled", "#4E79A7"),
    ("interpreted", "Micronaut Jakarta EL, interpreted", "#A0CBE8"),
    ("expressly", "Eclipse Expressly (reference implementation)", "#F28E2B"),
    ("tomcat", "Apache Tomcat Jasper EL", "#E15759"),
]
BENCHMARKS = [
    ("property", "property: ${book.title}"),
    ("nestedProperty", "nested property: ${book.author.name}"),
    ("composite", "composite: Book: ${book.title} costs ${book.unitPrice}"),
    ("arithmetic", "arithmetic: ${book.unitPrice * 2 + 1 > 10 ? 'expensive' : 'cheap'}"),
    ("comparison", "comparison: ${book.pages >= 300 and book.author.born < 1980}"),
    ("methodCall", "method call: ${book.discounted(10)}"),
    ("mapAccess", "map access: ${book.attributes['isbn']}"),
    ("stream", "stream: ${book.tags.stream().filter(t -> t.length() > 2).map(t -> t += '!').toList()}"),
    ("lambda", "lambda: ${(x -> x * 2 + book.pages)(book.pages)}"),
    ("math", "math: ${(book.unitPrice * book.pages - 100) / 3 mod 7 + -book.pages}"),
    ("complex", "complex: ${book.pages > 100 and book.unitPrice < 50 ? book.title += ' (' += ... : 'none'}"),
    ("listIndex", "list index: ${book.tags[1]}"),
    ("staticMethod", "static method: ${Integer.toHexString(book.pages)}"),
    ("stringMethods", "string methods: ${book.title.toUpperCase().substring(0, 3) += '...'}"),
    ("emptyCheck", "empty: ${empty book.tags or book.tags.size() > 2}"),
    ("dynamicBean", "undeclared bean: ${order.customer.name}"),
    ("customLambda", "custom functional interface: ${book.adjusted((p, q) -> p * q + 1)}"),
    ("createAndEvaluate", "create and evaluate: factory.createValueExpression(...).getValue(...)"),
]


def geomean(values):
    return math.exp(sum(math.log(v) for v in values) / len(values))


def aggregate(scores):
    """The geometric mean of the scores of each stack over the benchmarks, the aggregate the chart and the
    docs report: a ratio of geometric means is the mean of the per-benchmark ratios."""
    return {stack: geomean([scores[(name, stack)][0] for name, _ in BENCHMARKS if (name, stack) in scores])
            for stack, _, _ in STACKS}


def load(path):
    scores = {}
    with open(path) as f:
        for entry in json.load(f):
            name = entry["benchmark"].rsplit(".", 1)[1]
            stack = entry["params"]["stack"]
            metric = entry["primaryMetric"]
            scores[(name, stack)] = (metric["score"], metric["scoreError"], metric["scoreUnit"])
    return scores


def rounded_max(value):
    if value <= 0:
        return 1
    magnitude = 10 ** math.floor(math.log10(value))
    for factor in (1, 2, 2.5, 5, 10):
        if value <= factor * magnitude:
            return factor * magnitude
    return 10 * magnitude


def fmt(value):
    if value == 0:
        return "0"
    if value >= 100:
        return f"{value:,.0f}"
    if value >= 10:
        return f"{value:,.1f}"
    return f"{value:,.2f}"


def esc(text):
    return text.replace("&", "&amp;").replace("<", "&lt;").replace(">", "&gt;")


def render(scores, subtitle, aggregate_only=False):
    width = 1180
    left, right = 32, 32
    bar_h, gap = 18, 6
    panel_top = 44
    panel_h = panel_top + len(STACKS) * (bar_h + gap) + 34
    legend_h = 40
    header_h = 72
    panels = [("aggregate", "all benchmarks: geometric mean of the average times")]
    if not aggregate_only:
        panels += list(BENCHMARKS)
    height = header_h + legend_h + len(panels) * (panel_h + 10) + 20
    out = [f'<?xml version="1.0" encoding="UTF-8"?>',
           f'<svg xmlns="http://www.w3.org/2000/svg" width="{width}" height="{height}" viewBox="0 0 {width} {height}" role="img" aria-labelledby="title desc">',
           '  <title id="title">EvaluationBenchmark results</title>',
           '  <desc id="desc">JMH results of evaluating the same Jakarta Expression Language expressions with the compiled and the interpreted Micronaut Jakarta EL, Eclipse Expressly and Apache Tomcat Jasper EL.</desc>',
           '  <style>',
           '    .bg { fill: #ffffff; }',
           '    .title { font: 700 24px Arial, Helvetica, sans-serif; fill: #1f2937; }',
           '    .subtitle { font: 400 13px Arial, Helvetica, sans-serif; fill: #4b5563; }',
           '    .panel-title { font: 700 15px Arial, Helvetica, sans-serif; fill: #111827; }',
           '    .subtle { font: 400 12px Arial, Helvetica, sans-serif; fill: #6b7280; }',
           '    .label { font: 400 12px Arial, Helvetica, sans-serif; fill: #374151; }',
           '    .value { font: 700 11px Arial, Helvetica, sans-serif; fill: #111827; }',
           '    .tick { font: 400 11px Arial, Helvetica, sans-serif; fill: #6b7280; }',
           '    .legend { font: 400 12px Arial, Helvetica, sans-serif; fill: #374151; }',
           '    .grid { stroke: #e5e7eb; stroke-width: 1; }',
           '  </style>',
           f'  <rect class="bg" x="0" y="0" width="{width}" height="{height}"/>',
           f'  <text x="{left}" y="34" class="title">EvaluationBenchmark results</text>',
           f'  <text x="{left}" y="56" class="subtitle">{esc(subtitle)}</text>']
    x = left
    for _, label, color in STACKS:
        out.append(f'  <rect x="{x}" y="{header_h + 3}" width="13" height="13" fill="{color}" rx="2"/>')
        out.append(f'  <text x="{x + 20}" y="{header_h + 14}" class="legend">{esc(label)}</text>')
        x += 20 + 7 * len(label) + 28
    y = header_h + legend_h
    label_w = 300
    chart_x = left + label_w
    chart_w = width - chart_x - right - 90
    means = aggregate(scores)
    for name, title in panels:
        if name == "aggregate":
            values = [means[stack] for stack, _, _ in STACKS]
        else:
            values = [scores.get((name, stack), (0, 0, "ns/op"))[0] for stack, _, _ in STACKS]
        maximum = rounded_max(max(values))
        out.append(f'  <text x="{left}" y="{y + 18}" class="panel-title">{esc(title)}</text>')
        out.append(f'  <text x="{left}" y="{y + 34}" class="subtle">ns/op, lower is better</text>')
        base = y + panel_top
        for i, tick in enumerate((0, maximum / 2, maximum)):
            tx = chart_x + chart_w * tick / maximum
            out.append(f'  <line class="grid" x1="{tx:.1f}" y1="{base}" x2="{tx:.1f}" y2="{base + len(STACKS) * (bar_h + gap)}"/>')
            out.append(f'  <text x="{tx:.1f}" y="{base + len(STACKS) * (bar_h + gap) + 14}" class="tick" text-anchor="middle">{fmt(tick)}</text>')
        compiled = values[0]
        for (stack, label, color), value in zip(STACKS, values):
            if name == "aggregate":
                score, text = value, f"{fmt(value)} ns/op, {value / compiled:.1f}x the compiled stack"
            else:
                score, error, unit = scores.get((name, stack), (0, 0, "ns/op"))
                text = f"{fmt(score)} ± {fmt(error)} {unit}"
            bw = chart_w * score / maximum
            out.append(f'  <text x="{chart_x - 8}" y="{base + bar_h - 5}" class="label" text-anchor="end">{esc(label)}</text>')
            out.append(f'  <rect x="{chart_x}" y="{base}" width="{bw:.1f}" height="{bar_h}" fill="{color}" rx="2"/>')
            out.append(f'  <text x="{chart_x + bw + 6:.1f}" y="{base + bar_h - 5}" class="value">{esc(text)}</text>')
            base += bar_h + gap
        y += panel_h + 10
    out.append('</svg>')
    return "\n".join(out) + "\n"


def main():
    arguments = [argument for argument in sys.argv[1:] if argument != "--aggregate"]
    scores = load(arguments[0])
    subtitle = arguments[2] if len(arguments) > 2 else "JMH, average time"
    with open(arguments[1], "w") as f:
        f.write(render(scores, subtitle, "--aggregate" in sys.argv))


if __name__ == "__main__":
    main()
