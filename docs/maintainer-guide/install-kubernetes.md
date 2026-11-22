---
author: Martin Weise
---

## TL;DR

To install DBRepo in your existing cluster, download the
sample [
`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/blob/release-1.11/helm/dbrepo/values.yaml)
for your deployment and update the variables, especially `hostname`.

```shell
helm upgrade --install dbrepo \
  -n dbrepo \
  "oci://ghcr.io/dbrepo-project/helm/dbrepo" \
  --values ./values.yaml \
  --version "1.14.1" \
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

## Galera recovery and application traffic

For both database StatefulSets, readiness requires a primary component, a synced
local node and `wsrep_ready=ON`. The headless service still advertises peers for
discovery. The application service routes to ready pods. A responsive SQL
listener can stay alive while a node waits for quorum. The startup probe allows
30 minutes for initialization or state transfer. Increase its failure threshold
if your largest measured recovery takes longer.

When all pods are unready, inspect the Galera status and the previous container
logs on each node. After a full cluster outage, preserve the volumes and recover
the most advanced node before restoring traffic. A readiness probe cannot choose
a safe bootstrap node. See the [MariaDB quorum recovery procedure](https://mariadb.com/docs/galera-cluster/high-availability/resetting-the-quorum-cluster-bootstrap).

## Uninstall

Remove DBRepo by stopping and removing all pods and persistent volume claims:

```shell
helm uninstall \
  dbrepo \
  -n dbrepo
kubectl -n dbrepo \
  delete \
  pvc \
  --all
```

!!! danger "Further configuration is highly recommended"

    See "Security Disclaimer" above, this quick &amp; non-secure installation needs to be configured further to achieve
    basic security guarantees. Please visit 
    the [configuration](/infrastructures/dbrepo/1.13/maintainer-guide/configuration/) page in the next step to complete
    the installation.
