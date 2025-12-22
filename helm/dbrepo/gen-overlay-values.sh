#!/bin/bash
OUTPUT_FILE="./values.yaml"
if ! command -v rabbitmqadmin >/dev/null 2>&1; then
  echo "FATAL: rabbitmqadmin not found - install from https://github.com/rabbitmq/rabbitmqadmin-ng/releases"
  exit 1
fi
curl -fsSL https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/v1.13.2/helm/dbrepo/values.yaml > $OUTPUT_FILE
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
yq e -i ".brokerservice.auth.passwordHash=\"$(rabbitmqadmin passwords salt_and_hash $PW --table-style borderless | sed -n 2p | grep -oP "([^ ]+)" | sed -n 3p)\"" $OUTPUT_FILE
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
yq e -i ".storageservice.mariadb.auth.rootPassword=\"$(openssl rand -hex 16)\"" $OUTPUT_FILE
yq e -i ".ui.oidc.authSessionSecret=\"$(openssl rand -hex 48)\"" $OUTPUT_FILE
yq e -i ".ui.oidc.sessionSecret=\"$(openssl rand -hex 48)\"" $OUTPUT_FILE
PW=$(openssl rand -hex 16)
yq e -i ".ui.oidc.tokenKey=\"data:;base64,$(openssl enc -aes-256-cbc -k $PW -P -pbkdf2 --nosalt | sed -n 1p | grep -oP "([A-F0-9]+)" | base64 -w 0)\"" $OUTPUT_FILE
