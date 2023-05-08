###### FIRST STAGE ######
FROM keycloak/keycloak:21.0 as config
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=update-email

# Configure a database vendor
ENV KC_DB=mariadb

WORKDIR /opt/keycloak

COPY ./auth.keystore ./conf/server.keystore

RUN /opt/keycloak/bin/kc.sh build

###### SECOND STAGE ######
FROM redhat/ubi9-minimal as binary

RUN microdnf update -y && microdnf install -y curl-minimal libcurl-minimal

###### THIRD STAGE ######
FROM keycloak/keycloak:21.0 as runtime

COPY --from=config /opt/keycloak/ /opt/keycloak/
COPY --from=binary /usr/lib64 /usr/lib64
COPY --from=binary /usr/bin/curl /usr/bin/curl

USER root

COPY ./dbrepo-realm.json /opt/keycloak/data/import/dbrepo-realm.json

WORKDIR /app

COPY ./healthcheck.sh ./healthcheck.sh

ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo

ENV KC_DB=mariadb
ENV KC_DB_URL=jdbc:mariadb://metadata-db/fda
ENV KC_DB_USERNAME=${METADATA_USERNAME}
ENV KC_DB_PASSWORD=${METADATA_PASSWORD}
ENV KC_HOSTNAME=localhost

ENV KEYCLOAK_IMPORT=/opt/keycloak/data/import/dbrepo-realm.json
ENV KEYCLOAK_ADMIN=fda
ENV KEYCLOAK_ADMIN_PASSWORD=fda

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ["bash", "/app/healthcheck.sh"]

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start-dev", "--import-realm"]
