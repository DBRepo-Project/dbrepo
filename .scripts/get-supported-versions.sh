#!/bin/bash
set -euo pipefail

release_line="${1:-${DOC_VERSION:-}}"

if [[ -z "${release_line}" ]]; then
  echo "[ERROR] Missing release line. Pass it as the first argument or set DOC_VERSION." >&2
  exit 1
fi

versions="$(
  git tag --list "v${release_line}.*" \
    | sed 's/^v//' \
    | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' \
    | sort -uV \
    | awk 'NR == 1 { printf "%s", $0; next } { printf ", %s", $0 } END { print "" }'
)"

if [[ -z "${versions}" ]]; then
  echo "[ERROR] Failed to resolve supported versions for release line ${release_line}." >&2
  exit 1
fi

printf '%s\n' "${versions}"
