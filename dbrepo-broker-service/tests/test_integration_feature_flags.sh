#!/bin/bash
DOCKER_OPTS="--log-level ERROR"

function clean () {
  echo "[DEBUG] Shutting down environment ..."
  docker $DOCKER_OPTS compose down
  docker $DOCKER_OPTS rm $(docker volume ls -q) || true
  echo "[DEBUG] Starting new environment ..."
  docker $DOCKER_OPTS compose up -d dbrepo-broker-service
  echo "[DEBUG] Waiting 30s ..."
  sleep 30
}

# BeforeAll
clean

# Test
echo "[DEBUG] run test status_mqtt_succeeds"
if ! docker $DOCKER_OPTS exec dbrepo-broker-service rabbitmqctl status | grep -q "Interface.*mqtt"; then
  echo "[ERROR] Node is not listening to MQTT port" > /dev/stderr
  exit 1
fi

# Test
echo "[DEBUG] run test mqtt_succeeds"
if ! docker $DOCKER_OPTS exec dbrepo-broker-service rabbitmq-plugins is_enabled rabbitmq_mqtt | grep -q "rabbitmq_mqtt is enabled"; then
  echo "[ERROR] Plugin rabbitmq_mqtt is not enabled" > /dev/stderr
  exit 1
fi

echo "[INFO] Finished successfully"
exit 0
