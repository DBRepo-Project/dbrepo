###### FIRST STAGE ######
FROM keycloak/keycloak:21.0 as config
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_HTTP_RELATIVE_PATH=/api/auth

# Configure a database vendor
ENV KC_DB=mariadb

WORKDIR /opt/keycloak

COPY ./server.keystore ./conf/server.keystore

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

COPY ./docker-entrypoint.sh ./docker-entrypoint.sh
COPY ./disable-tls.sh ./disable-tls.sh

ENV AUTH_DB=keycloak
ENV AUTH_USERNAME=root
ENV AUTH_PASSWORD=dbrepo

ENV KC_DB=mariadb
ENV KC_DB_URL=jdbc:mariadb://auth-db/${AUTH_DB}
ENV KC_DB_USERNAME=${AUTH_USERNAME}
ENV KC_DB_PASSWORD=${AUTH_PASSWORD}
ENV KC_HOSTNAME_STRICT_HTTPS=false
ENV KC_HOSTNAME_PATH=/api/auth
ENV KC_HOSTNAME_ADMIN_URL=http://localhost/api/auth

ENV KEYCLOAK_IMPORT=/opt/keycloak/data/import/dbrepo-realm.json
ENV KEYCLOAK_ADMIN=fda
ENV KEYCLOAK_ADMIN_PASSWORD=fda

ENTRYPOINT [ "bash", "/app/docker-entrypoint.sh" ]
