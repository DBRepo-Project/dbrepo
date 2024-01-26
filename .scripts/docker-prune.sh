#!/bin/bash
if [[ "$CI_COMMIT_BRANCH" =~ (dev|master) ]]; then
  echo "pruning for branch ${CI_COMMIT_BRANCH} ..."
  docker system prune -f -a --volumes
fi