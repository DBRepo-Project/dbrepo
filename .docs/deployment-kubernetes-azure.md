---
author: Martin Weise
---

# Special Instructions for Azure Cloud

You can use our pre-built Helm chart for deploying DBRepo in your Kubernetes Cluster 
with Microsoft Azure as infrastructure provider.

## Requirements

### Hardware

For this small cloud, test deployment any public cloud provider would suffice, we recommend a 
small [Kubernetes Service](https://azure.microsoft.com/en-us/products/kubernetes-service)
with Kubernetes version *1.24.10* and node sizes *Standard_B4ms*

- 4 vCPU cores
- 16GB RAM memory
- 200GB SSD storage

This is roughly met by selecting the *Standard_B4ms* flavor and three worker nodes.

## Deployment

### Databases

Since Azure offers a managed [Azure Database for MariaDB](https://azure.microsoft.com/en-us/products/mariadb), we
recommend to at least deploy the Metadata Database as high-available, managed database.

!!! warning "End of Life software"

    Unfortunately, Azure does not (yet) support managed MariaDB 10.5, the latest version supported by Azure is 10.3
    which is End of Life (EOL) from [May 2023 onwards](https://mariadb.com/kb/en/changes-improvements-in-mariadb-10-3/).
    Microsoft decided to still maintain MariaDB 10.3
    until [September 2025](https://learn.microsoft.com/en-us/azure/mariadb/concepts-supported-versions).

### Fileshare

For the shared volume *PersistentVolumeClaim* `dbrepo-shared-volume-claim`, select an appropriate *StorageClass* that 
supports:

1. Access mode `ReadWriteMany`
2. Hardlinks (TUSd creates lockfiles during upload)

You will need to use a *StorageClass* of either `managed-*` or `azureblob-*` (after enabling the 
proprietary [CSI driver for BLOB storage](https://learn.microsoft.com/en-us/azure/aks/azure-blob-csi?tabs=NFS#azure-blob-storage-csi-driver-features)
in your Kubernetes Cluster).

We recommend to create 
a [Container](https://learn.microsoft.com/en-us/azure/storage/blobs/storage-blobs-introduction#containers) for the
[Upload Service](../system-services-upload) to deposit files and mount the BLOB storage
via CSI drivers into the *Deployment*. It greatly increases the available interfaces (see below) for file uploads and
provides a highly-available filesystem for the many deployments that need to use the files.
