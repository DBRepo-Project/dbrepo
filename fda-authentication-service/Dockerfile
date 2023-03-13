###### FIRST STAGE ######
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

###### SECOND STAGE ######
FROM keycloak/keycloak:21.0 as runtime
COPY --from=config /opt/keycloak/ /opt/keycloak/

USER root

COPY dbrepo-realm.json /dbrepo-realm.json

ENV METADATA_USERNAME=root
ENV METADATA_PASSWORD=dbrepo

ENV KC_DB=mariadb
ENV KC_DB_URL=jdbc:mariadb://fda-metadata-db/keycloak
ENV KC_DB_USERNAME=${METADATA_USERNAME}
ENV KC_DB_PASSWORD=${METADATA_PASSWORD}
ENV KC_HOSTNAME=localhost

ENV KEYCLOAK_IMPORT=/dbrepo-realm.json
ENV KEYCLOAK_ADMIN=keycloak
ENV KEYCLOAK_ADMIN_PASSWORD=keycloak

VOLUME /tmp

EXPOSE 9097

ENTRYPOINT ["/opt/keycloak/bin/kc.sh", "start"]
