#!/bin/bash
NAMESPACE=dbrepo

echo "Waiting for containers to start ..."
SUCCESS=0
for i in 0 1 2 3 4 5 6 7 8 9; do
  RES=$(kubectl -n ${NAMESPACE} get pods | awk 'NR>1 {print $3}' | grep -qF "[^Running|^Completed]")
  if [ "$?" -ne 0 ]; then
    if [ $SUCCESS -eq 0 ]; then
      echo "INFO: all pods started or completed: wait another iteration ..."
      sleep 30
      SUCCESS=1
      continue
    fi
    echo "INFO: all pods started or completed"
    exit 0
  fi
  echo "Waiting ..."
  sleep 30
done
echo "ERROR: some pods did not successfully complete or are still running"
exit 1