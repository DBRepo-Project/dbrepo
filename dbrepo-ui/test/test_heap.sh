#!/bin/bash
CALLS=1000
CONCURRENCY=10
ENDPOINT=http://localhost

ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/search"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/login"
ab -n "${CALLS}" -c "${CONCURRENCY}" "${ENDPOINT}/signup"