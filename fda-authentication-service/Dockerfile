###### FIRST STAGE ######
FROM maven:slim as build
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

WORKDIR /app

COPY ./rabbitmq-event-listener/pom.xml ./pom.xml

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY ./rabbitmq-event-listener ./

# Make sure it compiles
RUN mvn -q clean package -DskipTests > /dev/null

RUN mv ./target/rabbitmq-event-listener-*.jar ./target/rabbitmq-event-listener.jar

###### SECOND STAGE ######
FROM keycloak/keycloak:21.0 as config

COPY --from=build /app/target/rabbitmq-event-listener.jar /opt/keycloak/providers/rabbitmq-event-listener.jar

# Enable health and metrics support
ENV KC_HEALTH_ENABLED=true
ENV KC_METRICS_ENABLED=true
ENV KC_FEATURES=account-api

# Configure a database vendor
ENV KC_DB=mariadb

WORKDIR /opt/keycloak

# for demonstration purposes only, please make sure to use proper certificates in production instead
RUN keytool -genkeypair -storepass password -storetype PKCS12 -keyalg RSA -keysize 2048 -dname "CN=server" -alias server -ext "SAN:c=DNS:localhost,IP:127.0.0.1" -keystore conf/server.keystore
RUN /opt/keycloak/bin/kc.sh build

###### THIRD STAGE ######
FROM redhat/ubi9-minimal as binary

RUN microdnf update -y && microdnf install -y curl-minimal libcurl-minimal

###### FOURTH STAGE ######
FROM keycloak/keycloak:21.0 as runtime

COPY --from=config /opt/keycloak/ /opt/keycloak/
COPY --from=binary /usr/lib64 /usr/lib64
COPY --from=binary /usr/bin/curl /usr/bin/curl
COPY --from=build /app/target/rabbitmq-event-listener.jar /opt/keycloak/providers/rabbitmq-event-listener.jar

USER root

COPY ./dbrepo-realm.json /opt/keycloak/data/import/dbrepo-realm.json

WORKDIR /app

COPY ./service-register.sh ./service-register.sh
COPY ./docker-entrypoint.sh ./docker-entrypoint.sh
COPY ./healthcheck.sh ./healthcheck.sh

ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo

ENV KC_DB=mariadb
ENV KC_DB_URL=jdbc:mariadb://fda-metadata-db/keycloak
ENV KC_DB_USERNAME=${METADATA_USERNAME}
ENV KC_DB_PASSWORD=${METADATA_PASSWORD}
ENV KC_HOSTNAME=localhost

ENV KEYCLOAK_IMPORT=/opt/keycloak/data/import/dbrepo-realm.json
ENV KEYCLOAK_ADMIN=keycloak
ENV KEYCLOAK_ADMIN_PASSWORD=keycloak

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD ["bash", "/app/healthcheck.sh"]

ENTRYPOINT ["bash", "/app/docker-entrypoint.sh"]
