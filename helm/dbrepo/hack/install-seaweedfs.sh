#!/bin/bash
helm upgrade -n seaweedfs seaweedfs https://seaweedfs.github.io/seaweedfs-csi-driver/helm/seaweedfs-csi-driver-0.1.3.tgz \
  --install --create-namespace