# DBRepo Helm chart

[DBRepo](https://www.ifs.tuwien.ac.at/infrastructures/dbrepo/__CHARTVERSION__/) is a database repository system that
allows researchers to ingest data into a central, versioned repository through common interfaces.

## TL;DR

Download the
sample [`values.yaml`](https://gitlab.phaidra.org/fair-data-austria-db-repository/fda-services/-/raw/master/helm-charts/dbrepo/values.yaml?inline=true)
for your deployment and update the variables, especially `hostname`.

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm/dbrepo" --values ./values.yaml --version "__CHARTVERSION__"
```

## Prerequisites

* Kubernetes 1.24+
* Kubernetes 3.8.0+
* PV provisioner support in the underlying infrastructure

## Installing the Chart

To install the chart with the release name `my-release`:

```bash
helm install my-release "oci://s210.dl.hpc.tuwien.ac.at/dbrepo/helm/dbrepo" --values ./values.yaml --version "__CHARTVERSION__"
```

The command deploys DBRepo on the Kubernetes cluster in the default configuration. The Parameters section lists the
parameters that can be configured during installation.

## Uninstalling the Chart

To uninstall/delete the `my-release` deployment:

```bash
helm delete my-release
```

The command removes all the Kubernetes components associated with the chart and deletes the release.

## Parameters

### Common parameters

| Name            | Description                          | Value           |
|-----------------|--------------------------------------|-----------------|
| `namespace`     | Namespace which DBRepo is running in | `""`            |
| `hostname`      | The hostname for ingress rules       | `""`            |
| `strategyType`  | Deployments update strategy          | `RollingUpdate` |
| `clusterDomain` | Internal cluster domain              | `cluster.local` |

