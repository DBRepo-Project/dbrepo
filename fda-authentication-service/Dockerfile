###### FIRST STAGE ######
FROM fda-metadata-db:latest as dependency
MAINTAINER Martin Weise <martin.weise@tuwien.ac.at>

###### SECOND STAGE ######
FROM keycloak/keycloak:21.0 as config

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
FROM maven:slim as build

COPY ./pom.xml ./

RUN mvn -fn -B dependency:go-offline > /dev/null

COPY --from=dependency /root/.m2/repository/at/tuwien /root/.m2/repository/at/tuwien

COPY ./rest-service ./rest-service
COPY ./services ./services
COPY ./report ./report

# Make sure it compiles
RUN mvn -q clean package -DskipTests

###### FOURTH STAGE ######
FROM keycloak/keycloak:21.0 as runtime
COPY --from=config /opt/keycloak/ /opt/keycloak/

USER root

COPY ./service_ready /usr/bin
COPY ./docker-entrypoint.sh /usr/bin
COPY dbrepo-keycloak-realm.json /dbrepo-keycloak-realm.json
RUN chmod +x /usr/bin/service_ready /usr/bin/docker-entrypoint.sh

ENV METADATA_DB=fda
ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo
ENV BROKER_USERNAME=fda
ENV BROKER_PASSWORD=fda
ENV WEBSITE=http://example.com
ENV GATEWAY_ENDPOINT=http://gateway-service:9095/api/broker
ENV TOKEN_MAX=5
ENV MAIL_FROM="Database Repository <noreply@example.com>"
ENV MAIL_REPLY_TO="Somebody <somebody@example.com>"
ENV MAIL_VERIFY=false
ENV JWT_ISSUER=dbrepo
ENV JWT_SECRET=secret
ENV JWT_EXPIRATION=86400000
ENV SMTP_HOST=""
ENV SMTP_PORT=""
ENV SMTP_USERNAME=""
ENV SMTP_PASSWORD=""
ENV LOG_LEVEL=debug

ENV KC_DB=mariadb
ENV KC_DB_URL=jdbc:mariadb://fda-metadata-db/keycloak
ENV KC_DB_USERNAME=${METADATA_USERNAME}
ENV KC_DB_PASSWORD=${METADATA_PASSWORD}
ENV KC_HOSTNAME=localhost

ENV KEYCLOAK_IMPORT=/dbrepo-keycloak-realm.json
ENV KEYCLOAK_ADMIN=keycloak
ENV KEYCLOAK_ADMIN_PASSWORD=keycloak

VOLUME /tmp

HEALTHCHECK --interval=10s --timeout=5s --retries=12 CMD service_ready

COPY --from=build ./rest-service/target/rest-service-*.jar ./authentication-service.jar

EXPOSE 9097

ENTRYPOINT ["/usr/bin/docker-entrypoint.sh"]
