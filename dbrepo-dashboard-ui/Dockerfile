FROM --platform=$BUILDPLATFORM docker.io/bitnami/grafana:11.5.3 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

COPY --chown=grafana:grafana ./grafana.ini ./conf/grafana.ini
COPY --chown=grafana:grafana ./ldap.toml ./conf/ldap.toml
COPY --chown=grafana:grafana ./dashboards /app/dashboards
COPY --chown=grafana:grafana ./provisioning ./conf/provisioning
