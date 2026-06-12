# Security Policy

## Scope

Use this policy to report security vulnerabilities in DBRepo.

It applies to the DBRepo source code, packaged libraries, container images, Helm charts, and deployment configuration maintained in this repository.

For vulnerabilities in third-party dependencies, local deployments, forks, plugins, or infrastructure outside this repository, please report to the respective maintainers first. We are happy to help coordinate when an issue also affects DBRepo upstream.

## Supported versions

We provide security fixes for the latest stable DBRepo release line. Older releases generally do not receive security backports. If you run an older release, upgrade to a maintained release.

Operators of DBRepo instances are encouraged to subscribe to release notifications, review release notes, and keep deployments up to date.

## Reporting a vulnerability

Please do not report security vulnerabilities in public issues, pull requests, or discussions.

Report vulnerabilities confidentially through [GitHub private vulnerability reporting](https://github.com/DBRepo-Project/dbrepo/security/advisories/new) or by email to [dbrepo@ulb.tu-darmstadt.de](mailto:dbrepo@ulb.tu-darmstadt.de).

Include as much of the following information as possible:

- a brief description of the vulnerability
- the affected DBRepo version, commit, image, chart, or package
- the affected component or service
- deployment details relevant to the issue
- steps to reproduce the vulnerability
- the expected and observed impact
- logs, screenshots, proof-of-concept code, or a minimal patch when useful

We aim to acknowledge vulnerability reports within 5 working days and provide an initial assessment within 10 working days.

## Coordinated disclosure

Give maintainers reasonable time to triage, fix, release, and notify operators before public disclosure.

When a vulnerability is confirmed, maintainers will confirm the scope, prepare a fix, plan the release, and coordinate disclosure with the reporter. Security fixes are usually published as part of a regular or patch release, depending on severity and urgency.

## Development practices

Maintainers are expected to follow secure development workflows:

- The default `main` branch is protected and requires changes through pull requests.
- Pull requests must be reviewed by another maintainer and have at least one approving review before merging.
- Pull requests must not be merged if the continuous integration workflows fail.
- Maintainers with write access must enable two-factor authentication on GitHub.
- Do not commit secrets, credentials, or private keys.

## Operator responsibility

DBRepo can be deployed in different environments and configurations. Operators are responsible for securing their own infrastructure, secrets, network exposure, backups, monitoring, and access controls.

Before running DBRepo in production, review the deployment documentation and security-related notes for the chosen installation method. Default or development configurations may not be appropriate for production use.

## Out of scope

The following reports are usually out of scope unless they demonstrate a reproducible vulnerability in DBRepo upstream:

- vulnerabilities caused only by local misconfiguration
- unsupported forks or modified deployments
- third-party services not maintained by DBRepo
- social engineering or physical attacks
- denial-of-service reports without a practical security impact
- reports generated only by automated scanners without analysis or reproduction steps
