#!/bin/bash
DOCKER_OPTS="--log-level ERROR"
S3CMD_CONFIG="$(pwd)/.s3cfg"
S3CMD_OPTS="--config=${S3CMD_CONFIG}"

export S3_ACCESS_KEY_ID="seaweedfsadmin"
export S3_BUCKET="dbrepo"
export S3_SECRET_ACCESS_KEY="seaweedfsadmin"
HOST_STORAGE_ENDPOINT="localhost:9000"

function get_lifecycle_xml () {
  s3cmd $S3CMD_OPTS getlifecycle s3://${S3_BUCKET} | sed -n '/^</,$p'
}

function lifecycle_xpath () {
  local xpath="$1"
  get_lifecycle_xml | xmllint --xpath "$xpath" -
}

function wait_for_init () {
  local attempts=30
  local status

  while [[ $attempts -gt 0 ]]; do
    status=$(docker inspect -f '{{.State.Status}}' dbrepo-storage-service-init 2>/dev/null || true)
    if [[ "$status" == "exited" ]]; then
      local exit_code
      exit_code=$(docker inspect -f '{{.State.ExitCode}}' dbrepo-storage-service-init)
      if [[ "$exit_code" -ne 0 ]]; then
        docker logs dbrepo-storage-service-init
        echo "[ERROR] storage init container exited with ${exit_code}" > /dev/stderr
        exit 1
      fi
      return 0
    fi
    sleep 2
    attempts=$((attempts - 1))
  done

  docker logs dbrepo-storage-service-init || true
  echo "[ERROR] storage init container did not finish in time" > /dev/stderr
  exit 1
}

function clean () {
  echo "[DEBUG] Shutting down environment ..."
  docker $DOCKER_OPTS compose down -v --remove-orphans || true
}

function run_init () {
  echo "[DEBUG] Starting storage init container ..."
  docker $DOCKER_OPTS compose up -d --build --force-recreate dbrepo-storage-service-init
  wait_for_init
}

# BeforeAll
clean
cat <<EOF > .s3cfg
access_key = ${S3_ACCESS_KEY_ID}
secret_key = ${S3_SECRET_ACCESS_KEY}
host_base = ${HOST_STORAGE_ENDPOINT}
host_bucket = ${S3_BUCKET}
use_https = False
signature_v2 = False
EOF

# Test
echo "[DEBUG] run test init_succeeds"
run_init
if ! s3cmd $S3CMD_OPTS ls s3:// | grep -q "s3://${S3_BUCKET}"; then
  echo "[ERROR] Failed to find bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test init_idempotent_succeeds"
run_init
if ! s3cmd $S3CMD_OPTS ls s3:// | grep -q "s3://${S3_BUCKET}"; then
  echo "[ERROR] Failed to find bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_enabled_succeeds"
XPATH="string((//*[local-name()='Rule']/*[local-name()='Status']/text())[1])"
RES=$(lifecycle_xpath "$XPATH")
if [[ $RES != "Enabled" ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_prefix_succeeds"
XPATH="string((//*[local-name()='Rule']//*[local-name()='Prefix']/text())[1])"
RES=$(lifecycle_xpath "$XPATH")
if [[ $RES != "" ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test find_expiry_config_expiration_days_succeeds"
XPATH="number((//*[local-name()='Rule']/*[local-name()='Expiration']/*[local-name()='Days']/text())[1])"
RES=$(lifecycle_xpath "$XPATH")
if [[ $RES -ne 1 ]]; then
  echo "[ERROR] Failed to find xpath $XPATH for bucket s3://${S3_BUCKET}" > /dev/stderr
  exit 1
fi

echo "[INFO] Finished successfully"
clean
exit 0
