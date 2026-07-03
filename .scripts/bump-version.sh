#!/bin/bash
set -euo pipefail

usage() {
  echo "Usage: $0 <new-version>"
  echo "  or:  $0 <new-version> <new-chart-version>"
  echo "  or:  $0 <old-version> <new-version> <new-chart-version>"
  echo ""
  echo "Reads current versions from the Makefile and detects the old version"
  echo "used across the codebase."
  echo "Examples:"
  echo "  $0 1.14.0                         # bump to 1.14.0"
  echo "  $0 1.14.0 1.14.0                  # bump with explicit chart version"
  echo "  $0 1.13.4 1.14.0 1.14.0           # explicit old app version override"
  exit 1
}

REPO_ROOT="$(git rev-parse --show-toplevel)"
cd "$REPO_ROOT"

# Read current versions from Makefile
CUR_DOC="$(grep -oP '^DOC_VERSION \?= \K.*' Makefile)"
CUR_APP="$(grep -oP '^APP_VERSION \?= \K.*' Makefile)"
CUR_CHART="$(grep -oP '^CHART_VERSION \?= \K.*' Makefile)"

if [[ $# -eq 1 ]]; then
  NEW_VERSION="$1"
  NEW_CHART="${NEW_VERSION}"
  OLD_VERSION="${CUR_APP}"
elif [[ $# -eq 2 ]]; then
  NEW_VERSION="$1"
  NEW_CHART="$2"
  OLD_VERSION="${CUR_APP}"
elif [[ $# -eq 3 ]]; then
  OLD_VERSION="$1"
  NEW_VERSION="$2"
  NEW_CHART="$3"
else
  usage
fi

# If OLD_VERSION is empty (e.g. OLD="" from Makefile), fall back to CUR_APP
if [[ -z "${OLD_VERSION:-}" ]]; then
  OLD_VERSION="${CUR_APP}"
fi

# If NEW_CHART is empty (e.g. CHART="" from Makefile), default to NEW_VERSION
if [[ -z "${NEW_CHART:-}" ]]; then
  NEW_CHART="${NEW_VERSION}"
fi

if ! [[ "$NEW_VERSION" =~ ^[0-9]+\.[0-9]+\.[0-9]+$ ]]; then
  echo "ERROR: Version must be in X.Y.Z format (e.g., 1.14.0)"
  exit 1
fi

DOC_VERSION="${NEW_VERSION%.*}"

echo "Bumping:"
echo "  DOC_VERSION: ${CUR_DOC} → ${DOC_VERSION}"
echo "  APP_VERSION: ${CUR_APP} → ${NEW_VERSION}"
echo "  CHART_VERSION: ${CUR_CHART} → ${NEW_CHART}"
echo "  Old version in other files: ${OLD_VERSION}"
echo ""

# --- 1. Makefile ---
echo "[1/7] Makefile"
sed -i "s/^DOC_VERSION ?= ${CUR_DOC}/DOC_VERSION ?= ${DOC_VERSION}/" Makefile
sed -i "s/^APP_VERSION ?= ${CUR_APP}/APP_VERSION ?= ${NEW_VERSION}/" Makefile
sed -i "s/^CHART_VERSION ?= ${CUR_CHART}/CHART_VERSION ?= ${NEW_CHART}/" Makefile

# --- 2. CI / release configs ---
echo "[2/7] GitHub Actions and GitLab CI"
for f in .github/workflows/release.yml .github/workflows/ci.yml; do
  sed -i "s/APP_VERSION: '${OLD_VERSION}'/APP_VERSION: '${NEW_VERSION}'/" "$f"
  sed -i "s/DOC_VERSION: '${CUR_DOC}'/DOC_VERSION: '${DOC_VERSION}'/" "$f"
  sed -i "s/CHART_VERSION: '${CUR_CHART}'/CHART_VERSION: '${NEW_CHART}'/" "$f"
done

sed -i "s/DOC_VERSION: \"${CUR_DOC}\"/DOC_VERSION: \"${DOC_VERSION}\"/" .gitlab-ci.yml
sed -i "s/APP_VERSION: \"${OLD_VERSION}\"/APP_VERSION: \"${NEW_VERSION}\"/" .gitlab-ci.yml
sed -i "s/CHART_VERSION: \"${CUR_CHART}\"/CHART_VERSION: \"${NEW_CHART}\"/" .gitlab-ci.yml
sed -i "s/${CUR_DOC}\./${DOC_VERSION}./g" .gitlab-ci.yml

# --- 3. Java core library ---
echo "[3/7] lib/java/dbrepo-core"
sed -i "s|<version>${OLD_VERSION}</version>|<version>${NEW_VERSION}</version>|" lib/java/dbrepo-core/pom.xml
sed -i "s|/dbrepo/${CUR_DOC}/|/dbrepo/${DOC_VERSION}/|g" lib/java/dbrepo-core/pom.xml

# --- 4. Helm chart ---
echo "[4/7] helm/dbrepo"
sed -i "s|:${CUR_CHART}|:${NEW_CHART}|g" helm/dbrepo/Chart.yaml
sed -i "s/version: \"${CUR_CHART}\"/version: \"${NEW_CHART}\"/" helm/dbrepo/Chart.yaml
sed -i "s/appVersion: \"${CUR_APP}\"/appVersion: \"${NEW_VERSION}\"/" helm/dbrepo/Chart.yaml
sed -i "s|/dbrepo/${CUR_DOC}/|/dbrepo/${DOC_VERSION}/|g" helm/dbrepo/Chart.yaml

sed -i "s|:${CUR_CHART}|:${NEW_CHART}|g" helm/dbrepo/values.yaml
sed -i "s|:${CUR_CHART}|:${NEW_CHART}|g" helm/dbrepo/overlay-values.yaml
sed -i "s|raw/v${OLD_VERSION}/|raw/v${NEW_VERSION}/|" helm/dbrepo/gen-overlay-values.sh

# --- 5. Scripts ---
echo "[5/7] .scripts"
sed -i "s/APP_VERSION=\"${OLD_VERSION}\"/APP_VERSION=\"${NEW_VERSION}\"/" .scripts/install.sh
sed -i "s|dbrepo-core-${OLD_VERSION}|dbrepo-core-${NEW_VERSION}|g" .scripts/build-java-lib.sh
sed -i "s|-Dversion=${OLD_VERSION}|-Dversion=${NEW_VERSION}|g" .scripts/build-java-lib.sh

# --- 6. versions.json ---
echo "[6/7] versions.json"
python3 -c "
import json
with open('versions.json') as f:
    data = json.load(f)
for entry in data:
    if 'latest' in entry.get('aliases', []):
        entry['aliases'] = []
data.insert(0, {'version': '${DOC_VERSION}', 'title': '${DOC_VERSION}', 'aliases': ['latest']})
with open('versions.json', 'w') as f:
    json.dump(data, f, indent=2)
    f.write('\n')
"

# --- 7. mkdocs.yml + Python doc URLs + Service POM files ---
echo "[7/7] mkdocs.yml, Python doc URLs, and service POM files"
sed -i "s|/dbrepo/${CUR_DOC}/|/dbrepo/${DOC_VERSION}/|g" mkdocs.yml
sed -i "s|cover_subtitle: Documentation for version v${CUR_APP}|cover_subtitle: Documentation for version v${NEW_VERSION}|" mkdocs.yml
sed -i "s|dbrepo_v${CUR_APP}\\.pdf|dbrepo_v${NEW_VERSION}.pdf|" mkdocs.yml
sed -i "s|default: ${CUR_DOC}|default: ${DOC_VERSION}|" mkdocs.yml

# Python doc URLs (but NOT the Python lib version — handled by bump-python)
sed -i "s|/dbrepo/${CUR_DOC}/|/dbrepo/${DOC_VERSION}/|g" lib/python/pyproject.toml
sed -i "s/doc_version = os.environ.get(\"DOC_VERSION\", \"${CUR_DOC}\")/doc_version = os.environ.get(\"DOC_VERSION\", \"${DOC_VERSION}\")/" lib/python/setup.py

for svc in dbrepo-consumer-service dbrepo-data-service dbrepo-metadata-service; do
  find "$svc" -name pom.xml -exec sed -i "s|<version>${OLD_VERSION}</version>|<version>${NEW_VERSION}</version>|g" {} +
done

echo ""
echo "Done! Bumped: DOC ${CUR_DOC}→${DOC_VERSION}, APP ${CUR_APP}→${NEW_VERSION}, CHART ${CUR_CHART}→${NEW_CHART}"
echo "Review the changes with: git diff"
