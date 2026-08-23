#!/usr/bin/env bash
# Regenerates the benchmark chart and the result tables of the README and the user guide from the last JMH run.
# Usage: benchmarks/refresh.sh "<environment and JMH settings, one line>"
set -euo pipefail
cd "$(dirname "$0")/.."
RESULTS=benchmarks/build/results/jmh/results.json
SUBTITLE=${1:-"JMH, average time"}
python3 benchmarks/chart.py "$RESULTS" benchmarks/evaluation-benchmark-results.svg "$SUBTITLE"
python3 benchmarks/chart.py "$RESULTS" benchmarks/evaluation-benchmark-aggregate.svg "$SUBTITLE" --aggregate
cp benchmarks/evaluation-benchmark-results.svg benchmarks/evaluation-benchmark-aggregate.svg src/main/docs/resources/img/
python3 - "$RESULTS" "$SUBTITLE" <<'PY'
import re, subprocess, sys
results, subtitle = sys.argv[1], sys.argv[2]
def table(kind):
    return subprocess.check_output(["python3", "benchmarks/results.py", results, kind], text=True).rstrip()
readme = open("benchmarks/README.md").read()
body = f"\n{subtitle}.\n\n{table('markdown')}\n\n![EvaluationBenchmark local results](evaluation-benchmark-results.svg)\n"
readme = re.sub(r"<!-- results:start -->.*<!-- results:end -->", "<!-- results:start -->" + body + "<!-- results:end -->", readme, flags=re.S)
open("benchmarks/README.md", "w").write(readme)
guide = open("src/main/docs/guide/whyCompiled.adoc").read()
body = f"\n{subtitle}:\n\n{table('asciidoc-aggregate')}\n"
guide = re.sub(r"// results:start.*// results:end", "// results:start" + body + "// results:end", guide, flags=re.S)
open("src/main/docs/guide/whyCompiled.adoc", "w").write(guide)
PY
echo "refreshed"
