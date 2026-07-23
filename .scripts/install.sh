#!/bin/bash

# preset
APP_VERSION="1.14.0"
MIN_CPU=8
MIN_RAM=10
SKIP_CHECKS=${SKIP_CHECKS:-0}
DOWNLOAD_ONLY=${DOWNLOAD_ONLY:-0}

# checks
if [[ $SKIP_CHECKS -eq 0 ]] && [[ $DOWNLOAD_ONLY -ne 1 ]]; then
  echo "[✨] Startup check ..."
  docker info > /dev/null
  if [[ $? -ne 0 ]]; then
    echo "Docker is not installed (or accessible in bash) on your system:"
    echo ""
    echo "  - install docker from https://docs.docker.com/desktop/install/linux-install/"
    echo "  - make sure the docker executable is in \$PATH"
    exit 2
  else
    echo "$(docker --version) OK"
  fi
  CPU=$(cat /proc/cpuinfo | grep processor | wc -l)
  if [[ $CPU -lt $MIN_CPU ]]; then
    echo "You do not have enough vCPU resources available:"
    echo ""
    echo "  - we found ${CPU} vCPU cores instead of necessary ${MIN_CPU}"
    echo "  - if you believe this is a mistake, skip startup checks with the SKIP_CHECKS=1 flag"
    exit 3
  else
    echo "vCPU ${CPU} OK"
  fi
  RAM=$(free -g -t | awk 'NR==2 {print $7}')
  if [[ $RAM -lt $MIN_RAM ]]; then
    echo "You do not have enough RAM free resources:"
    echo ""
    echo "  - we found ${RAM}GB RAM (free) instead of necessary ${MIN_RAM}GB"
    echo "  - if you believe this is a mistake, skip startup checks with the SKIP_CHECKS=1 flag"
    exit 4
  else
    echo "RAM ${RAM}GB OK"
  fi
else
  echo "[✨] Skipping checks ..."
fi

# environment
echo "[🚀] Gathering environment for version ${APP_VERSION} ..."
curl -ksSL -o ./dist.tar.gz "https://github.com/DBRepo-Project/dbrepo/archive/refs/tags/${APP_VERSION}.tar.gz"
tar xzfv ./dist.tar.gz

if [[ $DOWNLOAD_ONLY -eq 1 ]]; then
  echo "[🎉] Successfully downloaded environment!"
  exit 0
fi

echo "[📦] Pulling images for version ${APP_VERSION} ..."
docker compose pull

INSTALL_SCRIPT=1 bash ./config/gen-secrets.sh

echo "[🎉] Success!"
echo ""
echo "You can now:"
echo ""
echo "  1) Either start the deployment running on http://localhost, or"
echo "  2) Edit the BASE_URL variable in .env to set your hostname"
echo ""
echo "Then start the local deployment with:"
echo ""
echo "  docker compose up -d"
echo ""
echo "Read about next steps online: https://dbrepo-project.github.io/dbrepo-docs/${APP_VERSION}/installation/#next-steps"
