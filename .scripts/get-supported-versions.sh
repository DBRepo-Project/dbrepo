#!/bin/bash
set -euo pipefail

version_input="${1:-${DOC_VERSION:-}}"

if [[ -z "${version_input}" ]]; then
  echo "[ERROR] Missing release line. Pass it as the first argument or set DOC_VERSION." >&2
  exit 1
fi

release_line="${version_input}"
if [[ "${version_input}" =~ ^([0-9]+)\.([0-9]+)\.[0-9]+$ ]]; then
  release_line="${BASH_REMATCH[1]}.${BASH_REMATCH[2]}"
fi

versions="$(
  git tag --list "v${release_line}.*" \
    | sed 's/^v//' \
    | grep -E '^[0-9]+\.[0-9]+\.[0-9]+$' \
    | sort -uV \
    | awk 'NR == 1 { printf "%s", $0; next } { printf ", %s", $0 } END { print "" }'
)"

printf '%s\n' "${versions}"
