---
author: Martin Weise
---

## TL;DR

To install DBRepo in your existing cluster, download the
sample [
`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.10/helm/dbrepo/values.yaml)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://registry.datalab.tuwien.ac.at/dbrepo/helm/dbrepo" \
  --values ./values.yaml \
  --version "1.10.0" \
  --create-namespace \
  --cleanup-on-fail
```

## Prerequisites

!!! info

     * Kubernetes 1.30+ (tested on 1.31.5)
     * PV provisioner support in the underlying infrastructure

### Resource Quota

By default, any service requests a minimum quota of cpu and memory by default. These values are based on the absolute
minimum needed to start the service. From our experience your cluster needs to meet the following `ResourceQuota`:

```yaml
requests.cpu: 12000m
requests.ephemeral-storage: 3072Mi
secrets: '50'
<storageClass>.storageclass.storage.k8s.io/persistentvolumeclaims: '20'
<storageClass>.storageclass.storage.k8s.io/requests.storage: 150Gi
persistentvolumeclaims: '20'
requests.memory: 14336Mi
pods: '40'
requests.storage: 150Gi
limits.memory: 20480Mi
configmaps: '20'
services: '40'
```
