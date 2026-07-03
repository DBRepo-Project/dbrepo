---
author: Martin Weise
---

# Release Process

This document describes how to create a new release of DBRepo.

## Overview

Releases follow [Semantic Versioning](https://semver.org/spec/v2.0.0.html) with the format `vX.Y.Z`. The release is cut from a `release-X.Y` branch. The CI/CD pipeline (GitHub Actions + GitLab CI) handles building and publishing Docker images, Helm charts, and the Python library.

Release branches: `release-X.Y` (e.g., `release-1.14`)
Tags: `vX.Y.Z` (e.g., `v1.14.0`)

## Prerequisites

- Write access to the repository
- [Docker](https://docs.docker.com/engine/install/) installed
- Python 3.11+ with `pipenv` for Python library publishing
- Docker Hub or GHCR login for image publishing (handled by CI)

## Step-by-step

### 1. Update the changelog

Add a new entry at the top of `docs/dev/changelog.md` for the new version. Follow the [Keep a Changelog](https://keepachangelog.com/en/1.1.0/) format:

```markdown
## [v1.14.0](https://github.com/DBRepo-Project/dbrepo/releases/tag/v1.14.0) - 2026-07-03

### Features

* Describe new features here.

### Fixes

* Describe bug fixes here.

### Changes

* Describe breaking or notable changes here.
```

### 2. Bump version references

If all versions are in sync (normal case):

```shell
make bump VERSION=1.14.0
```

If the Python library needs a different version (it follows its own scheme):

```shell
make bump VERSION=1.14.0
make bump-python VERSION=2.0.0
```

If the chart version differs from the app version:

```shell
make bump VERSION=1.14.0 CHART=1.14.0
```

If the Makefile's `APP_VERSION` is out of sync with the rest of the codebase,
pass the actual old version explicitly:

```shell
make bump VERSION=1.14.0 OLD=1.13.4
```

These commands update the following files:

| Area | Files |
|---|---|
| Makefile | `DOC_VERSION`, `APP_VERSION`, `CHART_VERSION` |
| CI configs | `.github/workflows/ci.yml`, `.github/workflows/release.yml`, `.gitlab-ci.yml` |
| Java library | `lib/java/dbrepo-core/pom.xml` |
| Java services | all `pom.xml` under `dbrepo-*-service/` |
| Helm chart | `helm/dbrepo/Chart.yaml`, `values.yaml`, `gen-overlay-values.sh` |
| Scripts | `.scripts/install.sh`, `.scripts/build-java-lib.sh` |
| Docs | `mkdocs.yml`, `versions.json` |
| Python lib\* | `lib/python/pyproject.toml`, `lib/python/setup.py`, `dbrepo-*-service/Pipfile*` |

\*Only when running `make bump-python`.

### 3. Review the changes

```shell
git diff --stat
```

Verify that all version references are updated correctly:

```shell
make version        # should print the new APP_VERSION
grep 'version' lib/python/pyproject.toml   # Python lib version
grep 'version' helm/dbrepo/Chart.yaml      # chart and appVersion
```

### 4. Commit the bump

```shell
git add -A
git commit -m "chore: bump version to 1.14.0"
```

### 5. Push the release branch

The release branch should already exist (`release-X.Y`). If not, create it:

```shell
git checkout -b release-1.14
git push origin release-1.14
```

If the branch already exists, merge main and push:

```shell
git checkout release-1.14
git merge main
git push origin release-1.14
```

Pushing to a `release-*` branch triggers the CI verify jobs (install script test, image existence checks).

### 6. Tag the release

```shell
git tag v1.14.0
git push origin v1.14.0
```

Pushing a tag triggers the full release pipeline:

- **Docker images**: Pulls SHA-tagged images from GHCR, retags them with the version, and pushes to GHCR (and previously Docker Hub).
- **Helm chart**: Updates `Chart.yaml`, packages and pushes to `oci://ghcr.io/dbrepo-project/helm`.
- **Python library**: Builds and publishes to [PyPI](https://pypi.org/project/dbrepo/).

### 7. Verify the release

Check that the following artifacts were published successfully:

- [ ] Docker images on [GHCR](https://github.com/orgs/DBRepo-Project/packages) — all services tagged with `v1.14.0`
- [ ] Helm chart on `oci://ghcr.io/dbrepo-project/helm/dbrepo`
- [ ] Python package on [PyPI](https://pypi.org/project/dbrepo/)
- [ ] Install script works: `curl -sSL https://raw.githubusercontent.com/DBRepo-Project/dbrepo/release-1.14/.scripts/install.sh | bash`

### 8. Update documentation

After the tag is pushed, deploy the docs for the new version:

```shell
make gen-dbrepo-doc
```

This updates the [documentation site](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/) with the new version.

## Release candidates

For release candidates, use the `-rcN` suffix:

```shell
git tag v1.14.0-rc1
git push origin v1.14.0-rc1
```

You can install a release candidate via the install script by setting
`APP_VERSION`:

```shell
APP_VERSION=1.14.0-rc1 bash ./.scripts/install.sh
```

## Hotfixes

For hotfixes on an existing release line, branch from the release tag,
fix, then tag with a `-fix` suffix:

```shell
git checkout -b hotfix-1.13 v1.13.5
# apply the fix
git commit -m "fix: ..."
git tag v1.13.6
git push origin v1.13.6
```

## CI/CD pipeline reference

### GitHub Actions

| Workflow | Trigger | What it does |
|---|---|---|
| `ci.yml` | PRs, pushes to `main`/`release-*` | Builds, tests, publishes SHA-tagged images to GHCR |
| `release.yml` | Tags, `release-*` branches | Retags SHA images with version, publishes Helm chart and Python lib |

### GitLab CI

| Stage | Trigger | What it does |
|---|---|---|
| `release-images` | Tags | Builds and pushes Docker images |
| `release-helm` | Tags | Signs and pushes Helm chart |
| `release-docs` | `release-*` branches | Deploys documentation via SSH |
| `release-libs` | Tags | Publishes Python library to PyPI |
| `verify-*` | `release-*` branches | Tests install script and verifies images |
