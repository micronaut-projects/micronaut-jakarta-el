#!/usr/bin/env bash
#
# Collects the evidence of a Jakarta Expression Language TCK run: the sanitized JUnit XML, a summary of the
# versions that were exercised, the checksums of the sanitized XML and an index page linking them together.
#
set -euo pipefail

results_dir="${1:?Usage: $0 <results-dir> <output-dir>}"
output_dir="${2:?Usage: $0 <results-dir> <output-dir>}"

mkdir -p "${output_dir}"

summary="${output_dir}/el-summary.md"
index="${output_dir}/index.html"
sha_file="${output_dir}/el-sha256.txt"
sanitized_dir="${output_dir}/junit-xml"
artifact="${output_dir}/el-evidence.tar.gz"
artifact_sha="${artifact}.sha256"

rm -rf "${sanitized_dir}"
mkdir -p "${sanitized_dir}"

# The timestamps, the host name and the durations differ between runs, remove them so that two runs of the
# same commit produce byte identical evidence.
if [[ -d "${results_dir}" ]]; then
  while IFS= read -r -d '' file; do
    relative="${file#"${results_dir}"/}"
    target="${sanitized_dir}/${relative}"
    mkdir -p "$(dirname "${target}")"
    sed \
      -e 's/ timestamp="[^"]*"/ timestamp="SANITIZED"/g' \
      -e 's/ hostname="[^"]*"/ hostname="SANITIZED"/g' \
      -e 's/ time="[^"]*"/ time="0"/g' \
      "${file}" > "${target}"
  done < <(find "${results_dir}" -name '*.xml' -type f -print0 | sort -z)
fi

count() {
  grep -ho "$1=\"[0-9]*\"" "${sanitized_dir}"/*.xml 2>/dev/null | cut -d\" -f2 | awk '{sum += $1} END {print sum + 0}'
}

tests="$(count tests)"
failures="$(count failures)"
errors="$(count errors)"
skipped="$(count skipped)"

workflow_url="not available"
if [[ -n "${GITHUB_SERVER_URL:-}" && -n "${GITHUB_REPOSITORY:-}" && -n "${GITHUB_RUN_ID:-}" ]]; then
  workflow_url="${GITHUB_SERVER_URL}/${GITHUB_REPOSITORY}/actions/runs/${GITHUB_RUN_ID}"
fi

property() {
  grep "^$1=" gradle.properties | cut -d= -f2-
}

{
  echo "# Jakarta Expression Language TCK Evidence"
  echo
  echo "- Commit: $(git rev-parse HEAD)"
  echo "- Project version: $(property projectVersion)"
  echo "- Expression Language API baseline: $(awk -F\" '/^managed-jakarta-el = "/ {print $2; exit}' gradle/libs.versions.toml)"
  echo "- TCK bundle: jakarta-expression-language-tck-$(property elTckVersion) ($(property elTckBranch) branch)"
  echo "- Workflow: ${workflow_url}"
  echo "- Sanitized JUnit XML: junit-xml/"
  echo "- Tests: ${tests}"
  echo "- Failures: ${failures}"
  echo "- Errors: ${errors}"
  echo "- Skipped: ${skipped}"
  echo
  echo "The signature test of the TCK is excluded: it verifies the signatures of the jakarta.el API jar, which"
  echo "this repository consumes unchanged rather than implements."
} > "${summary}"

find "${sanitized_dir}" -name '*.xml' -type f -print0 | sort -z | xargs -0 shasum -a 256 > "${sha_file}" || true

{
  echo "<!doctype html>"
  echo "<html lang=\"en\"><head><meta charset=\"utf-8\"><title>Jakarta Expression Language TCK Evidence</title></head><body>"
  echo "<h1>Jakarta Expression Language TCK Evidence</h1>"
  echo "<pre>"
  sed 's/&/\&amp;/g; s/</\&lt;/g' "${summary}"
  echo "</pre>"
  echo "<ul>"
  echo "<li><a href=\"$(basename "${summary}")\">Summary Markdown</a></li>"
  echo "<li><a href=\"$(basename "${sha_file}")\">Sanitized XML SHA-256</a></li>"
  echo "<li><a href=\"junit-xml/\">Sanitized JUnit XML</a></li>"
  echo "</ul>"
  echo "</body></html>"
} > "${index}"

tar -C "${output_dir}" -czf "${artifact}" "junit-xml" "$(basename "${summary}")" "$(basename "${sha_file}")" "$(basename "${index}")"
shasum -a 256 "${artifact}" > "${artifact_sha}"

echo "Jakarta Expression Language TCK: ${tests} tests, ${failures} failures, ${errors} errors, ${skipped} skipped"
