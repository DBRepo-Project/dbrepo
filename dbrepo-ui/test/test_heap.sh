#!/bin/bash
CALLS=${CALLS:-1000}
CONCURRENCY=${CONCURRENCY:-10}
ENDPOINT=${ENDPOINT:-http://localhost}

echo "[DEBUG] Testing endpoint: ${ENDPOINT} x${CALLS} (concurrency ${CONCURRENCY})"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/search"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/login"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/signup"