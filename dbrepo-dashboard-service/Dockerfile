FROM docker.io/bitnami/grafana:11.5.1 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

WORKDIR /app

COPY --chown=grafana:grafana ./dashboards /app/dashboards
COPY --chown=grafana:grafana ./grafana.ini /etc/grafana/grafana.ini
COPY --chown=grafana:grafana ./ldap.toml /etc/grafana/ldap.toml
