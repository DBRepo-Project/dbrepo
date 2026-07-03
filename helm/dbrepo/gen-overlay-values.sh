#!/bin/bash
OUTPUT_FILE="./overlay-values.yaml"
if ! command -v rabbitmqadmin >/dev/null 2>&1; then
  echo "FATAL: rabbitmqadmin not found - install from https://github.com/rabbitmq/rabbitmqadmin-ng/releases"
  exit 1
fi
curl -fsSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/v1.14.0/helm/dbrepo/values.yaml > $OUTPUT_FILE
yq e -i ".password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".metadatadb.rootUser.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".metadatadb.galera.mariabackup.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".dashboarddb.auth.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".authservice.auth.adminPassword=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".authservice.postgresql.auth.postgresPassword=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".cachedb.auth.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".datadb.rootUser.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".datadb.db.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".datadb.galera.mariabackup.password=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
PW=$(openssl rand -hex 16)
yq e -i ".brokerservice.auth.password=\"$PW\"" $OUTPUT_FILE
RABBIT_HASH=$(rabbitmqadmin passwords salt_and_hash "$PW" --table-style borderless | awk '/password hash/{print $NF}')
yq e -i ".brokerservice.auth.passwordHash=\"$RABBIT_HASH\"" $OUTPUT_FILE
PW=$(openssl rand -hex 16)
yq e -i ".brokerservice.ldap.bindpw=\"$PW\"" $OUTPUT_FILE
yq e -i ".identityservice.global.adminPassword=\"$PW\"" $OUTPUT_FILE
PW=$(openssl rand -hex 16)
yq e -i ".dataservice.s3.auth.accessKeyId=\"$PW\"" $OUTPUT_FILE
yq e -i ".metadataservice.s3.auth.accessKeyId=\"$PW\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.defaultAccessKeyId=\"$PW\"" $OUTPUT_FILE
PW=$(openssl rand -hex 16)
yq e -i ".dataservice.s3.auth.secretAccessKey=\"$PW\"" $OUTPUT_FILE
yq e -i ".metadataservice.s3.auth.secretAccessKey=\"$PW\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.defaultSecretAccessKey=\"$PW\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.adminAccessKeyId=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.adminSecretAccessKey=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.readAccessKeyId=\"$(yq e '.storageservice.s3.auth.defaultAccessKeyId' $OUTPUT_FILE)\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.readSecretAccessKey=\"$(yq e '.storageservice.s3.auth.defaultSecretAccessKey' $OUTPUT_FILE)\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.existingSecret=\"storage-service-secret\"" $OUTPUT_FILE
yq e -i ".storageservice.s3.auth.existingSecretConfigKey=\"config.json\"" $OUTPUT_FILE
yq e -i ".storageservice.mariadb.auth.rootPassword=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".ui.oidc.authSessionSecret=\"$(openssl rand -hex 48)\"" $OUTPUT_FILE
yq e -i ".ui.oidc.sessionSecret=\"$(openssl rand -hex 48)\"" $OUTPUT_FILE
OIDC_TOKEN_KEY=$(openssl rand -base64 32 | tr -d '\n')
yq e -i ".ui.oidc.tokenKey=\"data:;base64,$OIDC_TOKEN_KEY\"" $OUTPUT_FILE
