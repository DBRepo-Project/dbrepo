#!/bin/bash

# preset
VERSION="1.7.1"
MIN_CPU=8
MIN_RAM=4
MIN_MAP_COUNT=262144
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
    echo "  - we found ${RAM}GB RAM (free) instead of necessary ${RAM}GB"
    echo "  - if you believe this is a mistake, skip startup checks with the SKIP_CHECKS=1 flag"
    exit 4
  else
    echo "RAM ${RAM}GB OK"
  fi
  MAX_MAP_COUNT=$(cat /proc/sys/vm/max_map_count)
  if [[ $MAX_MAP_COUNT -lt $MIN_MAP_COUNT ]]; then
    echo "You do not have enough max. map counts: found ${MAX_MAP_COUNT} instead of minimum ${MIN_MAP_COUNT}"
    if [ $(id -u) -eq 0 ]; then
        echo "  - attempt to update the /etc/sysctl.conf file  and add the line 'vm.max_map_count=${MIN_MAP_COUNT}' at the end"
        echo "vm.max_map_count=${MIN_MAP_COUNT}" >> /etc/sysctl.conf
        sysctl -p
        if [[ $MAX_MAP_COUNT -lt $MIN_MAP_COUNT ]]; then
            exit 4
        fi
    else
        echo "  - you need to re-run the install.sh script as root to fix this"
        exit 4
    fi
  else
    echo "MAP COUNT ${MAX_MAP_COUNT} OK"
  fi
else
  echo "[✨] Skipping checks ..."
fi

# environment
echo "[🚀] Gathering environment for version ${VERSION} ..."
curl -ksSL -o ./dist.tar.gz "https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/${VERSION}/dist.tar.gz"
tar xzfv ./dist.tar.gz

if [[ $DOWNLOAD_ONLY -eq 1 ]]; then
  echo "[🎉] Successfully downloaded environment!"
  exit 0
fi

echo "[📦] Pulling images for version ${VERSION} ..."
docker compose pull

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
echo "Read about next steps online: https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/${VERSION}/installation/#next-steps"
