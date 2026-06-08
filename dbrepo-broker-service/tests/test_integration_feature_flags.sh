#!/bin/bash
DOCKER_OPTS="--log-level ERROR"

function clean () {
  echo "[DEBUG] Shutting down environment ..."
  docker $DOCKER_OPTS compose down
  docker $DOCKER_OPTS rm $(docker volume ls -q) || true
  echo "[DEBUG] Starting new environment ..."
  docker $DOCKER_OPTS compose up -d dbrepo-broker-service
  echo "[DEBUG] Waiting 60s ..."
  sleep 60
}

# BeforeAll
clean

# Test
echo "[DEBUG] run test status_mqtt_succeeds"
RES=$(docker $DOCKER_OPTS exec dbrepo-broker-service rabbitmqctl status)
if ! echo $RES | grep -q "Interface.*mqtt"; then
  echo "[ERROR] Node is not listening to MQTT port" > /dev/stderr
  echo "[DEBUG] result: $RES"
  exit 1
fi

# Test
echo "[DEBUG] run test mqtt_succeeds"
RES=$(docker $DOCKER_OPTS exec dbrepo-broker-service rabbitmq-plugins is_enabled rabbitmq_mqtt)
if ! echo $RES | grep -q "rabbitmq_mqtt is enabled"; then
  echo "[ERROR] Plugin rabbitmq_mqtt is not enabled" > /dev/stderr
  echo "[DEBUG] result: $RES"
  exit 1
fi

echo "[INFO] Finished successfully"
exit 0
