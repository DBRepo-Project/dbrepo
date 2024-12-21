## Contributing to DBRepo

All contributions are welcome. Ideas, code, documentation, production experiences, bugfixes... we only need to define
some key ideas in order to make this more efficient.

### Key ideas

* Follow the object-oriented programming principles for the Java-based services
* Embrace the [GitOps](https://opengitops.dev/) principles
* Simplicity first

### Development guide

We have a [development guide](./.docs/DEVELOPMENT.md) at your disposal so you can quickly setup an environment to
develop locally.

### Issues

Creating a new issue it's generally a good way to share your ideas and get feedback. We consider creating good issues
and documentation its part of the creative process, and we don't want to interfere on it. However, it's encouraged to
provide as much context as possible. Feel free to talk about a specific use case, show the maintainers what we are
trying to solve.

Explore the available labels in order to proper categorize it and get the fastest feedback.

If the contribution it's a bugfix, a little feature or documentation improvement that could be implemented in, lets say,
a couple of days at maximum, one could go directly for a PR. It's fine.

### Release checklist

- [ ] Change variables `APP_VERSION` and `CHART_VERSION` in CI/CD file `.gitlab-ci.yml`
- [ ] Change Helm chart variables in `helm/dbrepo/Chart.yaml` and update the chart README.md and values.schema.json for artifact hub with `make gen-helm-doc`
- [ ] Change Python library version in `lib/python/setup.py` and `lib/python/pyproject.toml` for PyPI
- [ ] Change the maven version in the metadata & data services:
  - `mvn -f ./dbrepo-metadata-service/pom.xml versions:set -DnewVersion=VERSION`
  - `mvn -f ./dbrepo-data-service/pom.xml versions:set -DnewVersion=VERSION`
- [ ] Change the versions in `versions.json` for the generated website

Then generate the REST API-, Python Library- and Helm Chart documentation:

```bash
# optional: pip install -r ./requirements.txt
make gen-swagger-doc gen-lib-doc gen-helm-doc
```