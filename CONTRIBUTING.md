# Contributing to DBRepo

DBRepo welcomes contributions from the community. Whether you report a bug, improve documentation, add tests, or work on a feature, your contribution helps improve the repository for data in databases.

Please read this guide before starting work. It explains how we use issues and pull requests, how to set up a local development environment, and what we expect from contributions.

## Code of conduct

Be respectful, constructive, and professional in all project spaces. We want DBRepo to be a welcoming project for contributors with different backgrounds, experience levels, and institutional contexts. To achieve this, we ask all contributors and maintainers to follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## Development process

DBRepo uses GitHub for source control, issue tracking, code review, and pull requests.

Before starting larger work, check whether an issue or pull request already exists. If there is no existing issue, create one first and describe the planned change. This helps avoid duplicated work and gives maintainers a chance to provide feedback early.

Trivial fixes such as typo corrections do not need prior discussion.

## Issues

Use issues for bug reports, feature requests, documentation problems, and technical discussions.

When reporting a bug, include:

- the DBRepo version or commit you are using
- the affected service, component, or deployment method
- your operating system and relevant tool versions
- steps to reproduce the problem
- what you expected to happen
- what happened instead
- logs, screenshots, or error messages when useful

Precise reproduction steps are the best way to help maintainers fix a bug quickly.

Do not report security vulnerabilities in public issues. See [Security](#security) instead.

## Pull requests

All code changes should be submitted as GitHub pull requests.

Keep pull requests small and focused. A good pull request addresses one feature, bug fix, documentation update, or technical concern. Avoid bundling unrelated refactoring, formatting changes, dependency updates, or generated files with functional changes.

Each pull request should:

- explain what changed and why
- link the related issue when one exists
- include tests for new behavior or bug fixes
- update documentation when user-facing behavior changes
- pass the relevant local checks before review
- pass required CI checks before merge

Review does not replace contributor testing. Please make the review process easy by describing how you tested the change.

## Commits and branches

Use small, logical commits. Each commit should represent one complete step that builds and passes the relevant tests.

DBRepo uses [Conventional Commits](https://www.conventionalcommits.org/en/v1.0.0/). Format commit messages as:

```text
<type>(<scope>): <description>
```

Examples:

```text
feat(metadata): add dataset export endpoint
fix(ui): preserve filters after reload
docs: add contributing guide
test(data): cover query permission checks
```

Common types are `feat`, `fix`, `docs`, `style`, `refactor`, `test`, `chore`, `perf`, `ci`, and `build`.

Use imperative, concise descriptions and avoid trailing periods. Prefer branch names that describe the change, for example `fix/query-permissions` or `docs/contributing-guide`.

## Coding style

Follow the style and patterns already used in the component you are changing. DBRepo is a polyglot project with Java services, Python libraries and services, a Nuxt/Vue user interface, Docker Compose files, and Helm charts.

Prefer readable, maintainable code over clever or overly compact solutions. Keep changes local to the affected component when possible. Create new abstractions only when they are needed, reusable, and tested.

Do not mix unrelated cleanup with functional changes. If you notice another problem while working, open a separate issue or pull request.

## AI-assisted contributions

AI-assisted coding is permitted, but contributors are responsible for everything they submit.

You must review, understand, test, and be able to explain AI-assisted changes. Submitting unverified AI output is not acceptable. If AI tooling was used substantially for a contribution, mention this in the pull request description, including the tools or models used when relevant.

## Development environment

Local development requires the following tools:

- Docker Engine `24.x` or newer
- K3S
- Java 21
- Maven
- Node.js, npm, and Bun for UI work
- Make

On Debian-based systems, the base packages can be installed with:

```shell
apt install maven openjdk-21-jdk make nodejs npm
```

Depending on the component you work on, additional tools such as Helm, kubectl, Python, or service-specific dependencies may be required.

## Build and run locally

Build the Java library when working on Java services or shared Java code:

```shell
make build-java-lib
```

Build the Python library when working on the Python client library:

```shell
make build-python-lib
```

Build the UI when working on the Nuxt/Vue frontend:

```shell
make build-ui
```

Build the Docker images and start the local development deployment:

```shell
make start-dev
```

Stop the local development deployment and remove local data with:

```shell
make stop-dev
```

## Testing

We practice test-driven development and expect contributors to test their changes. New features and bug fixes should include automated tests where practical.

Changes should maintain at least 80% code coverage for the affected component. Java-based services generate JaCoCo reports in the `report/site/` subdirectory. Python-based services use `coverage`, including the `.coverage` SQLite database and `coverage.txt` log file where applicable.

Run the tests relevant to your change before opening a pull request:

```shell
make test-data-service
make test-metadata-service
make test-analyse-service
make test-search-service
make test-lib
make test-ui
```

If you intentionally cannot run a relevant test locally, explain why in the pull request and describe any alternative verification you performed.

## Continuous integration

Required CI checks must pass before a pull request can be merged. If CI fails, fix the underlying problem rather than bypassing or weakening the check.

CI may run lint, build, test, documentation, verification, release, and scan jobs depending on the branch and pull request context.

## Documentation

Update documentation when behavior, configuration, deployment, APIs, or user-facing workflows change.

For consistency across documentation screenshots, use a resolution of `1280x800` with a `16:10` ratio.

Generated documentation, OpenAPI output, Helm chart documentation, and schema files should be regenerated with the existing project commands when your change affects them.

## Security

Do not disclose security vulnerabilities in public issues, pull requests, or discussions.

If you believe you found a vulnerability, contact the maintainers privately. Include enough detail to reproduce and assess the issue, but do not share exploit instructions publicly before the issue has been addressed.

## License

DBRepo is licensed under the Apache License 2.0. By contributing to DBRepo, you agree that your contribution is provided under the same license.
