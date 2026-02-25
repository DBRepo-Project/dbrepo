#!/bin/bash

function gen_rabbit_key() {
  rabbitmqadmin passwords salt_and_hash $1 --table-style borderless | sed -n 2p | grep -oP "([^ ]+)" | sed -n 3p
}

function gen_pw() {
  LENGTH=16
  if [ ! -z $1 ]; then
    LENGTH=$1;
  fi
  openssl rand -hex $LENGTH
}

BROKER_SERVICE_PASSWORD="admin"
RABBIT_HASH="dI+aj+LWJNHsKvowmxgHKBJhkXgZ67SX1XRPOBGjl5fnwbZB"
if ! [ -x "$(command -v rabbitmqadmin)" ]; then
  echo "[🚨] Cannot generate broker service user secret(s) as rabbitmqadmin seems not to be installed or could not be found"
else
  BROKER_SERVICE_PASSWORD=$(gen_pw)
  RABBIT_HASH=$(gen_rabbit_key $BROKER_SERVICE_PASSWORD)
fi

CERT_PATH=./config
if [ ! -z $IS_DEV ]; then
  CERT_PATH=./dbrepo-gateway-service
fi

function gen_aes_key() {
  openssl enc -aes-256-cbc -k secret -P -pbkdf2 --nosalt | sed -n 1p | grep -oP "([A-F0-9]+)" | base64 -w 0
}

echo "[🙈] Generating random secrets ..."
DATA_DB_PASSWORD=$(gen_pw)
READONLY_PASSWORD=$(gen_pw)
S3_ACCESS_KEY_ID=$(gen_pw)
S3_SECRET_ACCESS_KEY=$(gen_pw)
S3_ADMIN_ACCESS_KEY_ID=$(gen_pw)
S3_ADMIN_SECRET_ACCESS_KEY=$(gen_pw)
cat <<EOF > .env
#### SECRETS BELOW ARE AUTO-GENERATED ####
AUTH_DB_PASSWORD=$(gen_pw)
AUTH_SERVICE_ADMIN_PASSWORD=$(gen_pw)
BROKER_SERVICE_ERL_COOKIE=$(gen_pw)
BROKER_SERVICE_PASSWORD=$BROKER_SERVICE_PASSWORD
CACHE_DB_PASSWORD=$(gen_pw)
DASHBOARD_DB_PASSWORD=$(gen_pw)
DASHBOARD_UI_PASSWORD=$(gen_pw)
DATA_DB_PASSWORD=$DATA_DB_PASSWORD
#IDENTITY_SERVICE_ADMIN_PASSWORD= (auto-config yet supported)
METADATA_DB_BACKUP_PASSWORD=$(gen_pw)
METADATA_DB_PASSWORD=$(gen_pw)
NUXT_OIDC_TOKEN_KEY=data:;base64,$(gen_aes_key)
OIDC_AUTH_SESSION_SECRET=$(gen_pw 48)
OIDC_SESSION_SECRET=$(gen_pw 48)
READONLY_PASSWORD=$READONLY_PASSWORD
S3_ACCESS_KEY_ID=$S3_ACCESS_KEY_ID
S3_SECRET_ACCESS_KEY=$S3_SECRET_ACCESS_KEY
S3_ADMIN_ACCESS_KEY_ID=$S3_ADMIN_ACCESS_KEY_ID
S3_ADMIN_SECRET_ACCESS_KEY=$S3_ADMIN_SECRET_ACCESS_KEY
SYSTEM_PASSWORD=$(gen_pw)
EOF
SECRET_PATH="./dbrepo-storage-service"
if [[ $INSTALL_SCRIPT -eq 1 ]]; then
  SECRET_PATH="./config"
fi
cat <<EOF > $SECRET_PATH/s3_config.json
{
  "identities": [
    {
      "name": "default",
      "credentials": [
        {
          "accessKey": "$S3_ACCESS_KEY_ID",
          "secretKey": "$S3_SECRET_ACCESS_KEY"
        }
      ],
      "actions": [
        "Read",
        "Write",
        "List"
      ]
    },
    {
      "name": "admin",
      "credentials": [
        {
          "accessKey": "$S3_ADMIN_ACCESS_KEY_ID",
          "secretKey": "$S3_ADMIN_SECRET_ACCESS_KEY"
        }
      ],
      "actions": [
        "Read",
        "Write",
        "List",
        "Tagging",
        "Admin"
      ]
    }
  ]
}
EOF
SECRET_PATH="./dbrepo-broker-service"
if [[ $INSTALL_SCRIPT -eq 1 ]]; then
  SECRET_PATH="./config"
fi
cat <<EOF > $SECRET_PATH/definitions.json
{
  "bindings": [
    {
      "arguments": {},
      "destination": "dbrepo",
      "destination_type": "queue",
      "routing_key": "dbrepo.#",
      "source": "dbrepo",
      "vhost": "dbrepo"
    }
  ],
  "exchanges": [
    {
      "arguments": {},
      "auto_delete": false,
      "durable": true,
      "name": "dbrepo",
      "type": "topic",
      "vhost": "dbrepo"
    }
  ],
  "global_parameters": [],
  "parameters": [],
  "permissions": [
    {
      "user": "admin",
      "vhost": "dbrepo",
      "configure": ".*",
      "read": ".*",
      "write": ".*"
    }
  ],
  "policies": [],
  "queues": [
    {
      "arguments": {
        "x-queue-type": "quorum"
      },
      "auto_delete": false,
      "durable": true,
      "name": "dbrepo",
      "type": "quorum",
      "vhost": "dbrepo"
    }
  ],
  "rabbit_version": "3.10.25",
  "rabbitmq_version": "3.10.25",
  "topic_permissions": [],
  "users": [
    {
      "name": "admin",
      "password_hash": "$RABBIT_HASH",
      "tags": [
        "administrator"
      ]
    }
  ],
  "vhosts": [
    {
      "limits": [],
      "metadata": {
        "description": "Default virtual host",
        "tags": []
      },
      "name": "dbrepo"
    }
  ]
}
EOF

echo "[🙉] Generating self-signed TLS certificate ..."
rm -rf "$CERT_PATH/tls.key" "$CERT_PATH/tls.crt"
openssl req -x509 -newkey rsa:4096 -keyout "$CERT_PATH/tls.key" -out "$CERT_PATH/tls.crt" -sha256 -days 3650 -nodes -subj "/C=XX/ST=StateName/L=CityName/O=CompanyName/OU=CompanySectionName/CN=localhost"
sudo chown 1001:1001 "$CERT_PATH/tls.key" "$CERT_PATH/tls.crt"
sudo chmod 755 "$CERT_PATH/tls.key" "$CERT_PATH/tls.crt"

echo "[🙊] Generating database setup ..."
SECRET_PATH="./dbrepo-metadata-db"
if [[ $INSTALL_SCRIPT -eq 1 ]]; then
  SECRET_PATH="./config"
fi
cat <<EOF > $SECRET_PATH/2_setup-data.sql
BEGIN;
INSERT INTO \`mdb_containers\` (id, name, internal_name, image_id, host, port, ui_host, ui_port, privileged_username,
                              privileged_password, readonly_username, readonly_password)
VALUES ('6cfb3b8e-1792-4e46-871a-f3d103527203', 'mariadb-galera:11.3.2', 'mariadb_galera_11_3_2',
        'd79cb089-363c-488b-9717-649e44d8fcc5', 'data-db', 3306, 'localhost', 3306, 'root',
        '$DATA_DB_PASSWORD', 'readonly', '$READONLY_PASSWORD');
COMMIT;
EOF
