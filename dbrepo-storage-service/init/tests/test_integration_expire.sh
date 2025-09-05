#!/bin/bash
DOCKER_OPTS="--log-level ERROR"
S3CMD_OPTS="--config=/app/config/.s3cfg"

export S3_ACCESS_KEY_ID="seaweedfsadmin"
export S3_BUCKET="dbrepo"
export S3_SECRET_ACCESS_KEY="seaweedfsadmin"
export STORAGE_ENDPOINT="localhost:9000"

function clean () {
  echo "[DEBUG] Shutting down environment ..."
  docker $DOCKER_OPTS compose down
  docker $DOCKER_OPTS rm $(docker volume ls -q) || true
  echo "[DEBUG] Starting new environment ..."
  docker $DOCKER_OPTS compose up -d dbrepo-storage-service-init
  echo "[DEBUG] Waiting 10s ..."
  sleep 10
}

# BeforeAll
clean

# Test
echo "[DEBUG] run test init_succeeds"
bash ./dbrepo-storage-service/init/init.sh
if ! s3cmd $S3CMD_OPTS ls s3:// | grep -q "s3://${S3_BUCKET}"; then
  echo "[ERROR] Failed to find bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test init_idempotent_succeeds"
bash ./dbrepo-storage-service/init/init.sh
if ! s3cmd $S3CMD_OPTS ls s3:// | grep -q "s3://${S3_BUCKET}"; then
  echo "[ERROR] Failed to find bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_enabled_succeeds"
XPATH="string(//Rule/Status/text())"
RES=$(s3cmd $S3CMD_OPTS getlifecycle s3://${S3_BUCKET} | xmllint --xpath $XPATH -)
if [[ $RES != "Enabled" ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_prefix_succeeds"
XPATH="string(//Rule/Prefix/text())"
RES=$(s3cmd $S3CMD_OPTS getlifecycle s3://${S3_BUCKET} | xmllint --xpath $XPATH -)
if [[ $RES != "" ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_expiration_days_succeeds"
XPATH="number(//Rule/Expiration/Days/text())"
RES=$(s3cmd $S3CMD_OPTS getlifecycle s3://${S3_BUCKET} | xmllint --xpath $XPATH -)
if [[ $RES -ne 1 ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

echo "[INFO] Finished successfully"
exit 0
