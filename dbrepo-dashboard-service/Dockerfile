FROM docker.io/bitnami/grafana:11.2.0-debian-12-r4 AS runtime
LABEL org.opencontainers.image.authors="martin.weise@tuwien.ac.at"

WORKDIR /app

COPY ./grafana.ini /etc/grafana/grafana.ini
COPY ldap.toml /etc/grafana/ldap.toml
COPY provisioning/datasources/prometheus.yaml /etc/grafana/provisioning/datasources/datasource.yaml
#COPY --chown=999:999 ./system.yaml /var/lib/grafana/dashboards/system.yaml
