#!/bin/bash
kubectl apply -f https://github.com/cert-manager/cert-manager/releases/download/v${CERT_MANAGER_VERSION}/cert-manager.yaml
if [ $? -ne 0 ]; then
  echo "ERROR: Failed to install cert-manager" > /dev/stderr
else
  echo "SUCCESS: Installed cert-manager"
fi
cat <<EOF | kubectl apply -f -
apiVersion: cert-manager.io/v1
kind: ClusterIssuer
metadata:
  name: selfsigned-cluster-issuer
spec:
  selfSigned: {}
EOF
