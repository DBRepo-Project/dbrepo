#!/bin/bash
set -euo pipefail

usage() {
  echo "Usage: $0 <old-version> <new-version>"
  echo "  or:  $0 <new-version>   (reads old version from pyproject.toml)"
  echo ""
  echo "Examples:"
  echo "  $0 1.14.0                    # bump from current Python lib version"
  echo "  $0 1.13.4 1.14.0             # explicit old→new"
  exit 1
}

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

if [[ $# -eq 1 ]]; then
  NEW_VERSION="$1"
  CUR_VERSION="$(grep -oP '^version = "\K[^"]+' lib/python/pyproject.toml)"
elif [[ $# -eq 2 ]]; then
  CUR_VERSION="$1"
  NEW_VERSION="$2"
else
  usage
fi

if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: Version must be in X.Y.Z format (e.g., 1.14.0)"
  exit 1
fi

# The doc version for URLs always follows the major.minor of the app,
# not the Python lib. Keep the existing one.
CUR_DOC="$(grep -oP '^DOC_VERSION \?= \K.*' Makefile)"

echo "Bumping Python library: ${CUR_VERSION} → ${NEW_VERSION}"
echo ""

# --- 1. pyproject.toml ---
echo "[1/3] lib/python/pyproject.toml"
sed -i "s/version = \"${CUR_VERSION}\"/version = \"${NEW_VERSION}\"/" lib/python/pyproject.toml

# --- 2. setup.py ---
echo "[2/3] lib/python/setup.py"
sed -i "s/app_version = os.environ.get(\"APP_VERSION\", \"${CUR_VERSION}\")/app_version = os.environ.get(\"APP_VERSION\", \"${NEW_VERSION}\")/" lib/python/setup.py

# --- 3. Pipfiles + Pipfile.locks ---
echo "[3/3] Pipfile and Pipfile.lock references"
for f in dbrepo-search-service/Pipfile dbrepo-dashboard-service/Pipfile \
         dbrepo-search-service/Pipfile.lock dbrepo-dashboard-service/Pipfile.lock; do
  if grep -q "dbrepo-${CUR_VERSION}.tar.gz" "$f" 2>/dev/null; then
    sed -i "s|dbrepo-${CUR_VERSION}.tar.gz|dbrepo-${NEW_VERSION}.tar.gz|g" "$f"
  fi
done

echo ""
echo "Done! Python library bumped from ${CUR_VERSION} to ${NEW_VERSION}."
echo "Review the changes with: git diff"
