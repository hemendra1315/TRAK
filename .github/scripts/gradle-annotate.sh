#!/usr/bin/env bash
# Runs a Gradle command and, on failure, surfaces the key error lines as a GitHub
# check-run annotation so failures are readable without downloading the full log.
# Usage: .github/scripts/gradle-annotate.sh <label> <gradle args...>
set -uo pipefail

label="$1"; shift
log="gradle-${label}.log"

./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}

if [ "$status" -ne 0 ]; then
  {
    grep -E "^e: |^w: .*error|error:|Execution failed|Could not resolve|Could not find|Plugin \[|incompatible|requires|was compiled with" "$log" | head -40
    echo "----"
    grep -A 25 "What went wrong" "$log" | head -60
  } > "gradle-${label}-summary.txt"
  # Encode newlines for the workflow command; keep within annotation size limits.
  msg=$(head -c 15000 "gradle-${label}-summary.txt" | sed ':a;N;$!ba;s/%/%25/g;s/\r/%0D/g;s/\n/%0A/g')
  echo "::error title=${label} failed::${msg}"
fi
exit "$status"
