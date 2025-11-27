---
author: Martin Weise
---

Please check the [guide](/infrastructures/dbrepo/1.13/maintainer-guide/backup-data/) on how to perform a backup first.

## TL;DR

This page explains how to restore from a backup of a MariaDB Galera (
i.e. [Data Database](/infrastructures/dbrepo/1.13/dev/services/data-db/)
and [Metadata Database](/infrastructures/dbrepo/1.13/dev/services/metadata-db/)).

## Restore Data

There are multiple guides available on how to restore, we prioritize 
the [Helm Chart documentation](https://artifacthub.io/packages/helm/bitnami/mariadb-galera) over the 
official [MariaDB 11.3.2 documentation](https://mariadb.com/docs/server/server-usage/backup-and-restore/mariadb-backup/full-backup-and-restore-with-mariadb-backup)
as it simplifies many steps.

### Orderly Shutdown

This section follows the official MariaDB 11.3.2 documentation adapted to the Bitnami Chart. This is the case when some
(or all) nodes in the cluster fail to reach a quorum due to them switching to a non-primary state and refusing to serve
queries to protect data integrity[^1].

Follow the recommendation of the safe to bootstrap feature on each node N=`0`, `1` and `2` (or more). Look 
for `safe_to_bootstrap: 1`.

```shell
kubectl exec data-db-N -- cat \
    /bitnami/mariadb/data/grastate.dat
```

!!! bug "Use the daemon to read the InnoDB storage engine logs and report the last known transaction position"

    Alternatively, you can use the MySQL daemon `mysqld` to determine the sequence number:

    ```shell
    kubectl exec data-db-N -- mysqld --wsrep-recover
    ```

!!! bug "Use the PVCs directly and read the sequence number from the volumes"

    Alternatively, if you cannot use the pods, you can try to find the most advanced node directly in the PVCs:

    ```shell
    kubectl run -i --rm --tty volpod --overrides='
    {
        "apiVersion": "v1",
        "kind": "Pod",
        "metadata": {
            "name": "volpod"
        },
        "spec": {
            "containers": [{
                "command": [
                    "cat",
                    "/mnt/data/grastate.dat"
                ],
                "image": "bitnami/minideb",
                "name": "mycontainer",
                "volumeMounts": [{
                    "mountPath": "/mnt",
                    "name": "galeradata"
                }]
            }],
            "restartPolicy": "Never",
            "volumes": [{
                "name": "galeradata",
                "persistentVolumeClaim": {
                    "claimName": "data-data-db-N"
                }
            }]
        }
    }' --image="bitnami/minideb"
    ```

If there exists a node with `safe_to_bootstrap: 1`, use this node `M` to [bootstrap](#bootstrap) from. Otherwise, 
if **no** node is safe to bootstrap from, follow the next section of [unexpected shutdown](#unexpected-shutdown)
scenario.

Then tell the node `M` to form a new primary component by itself. This process is non-destructive and increases the
chance of a fast incremental State Transfer (IST) for the other nodes.

```shell
helm upgrade \
    --install \
    my-release \
    "oci://registry.datalab.tuwien.ac.at/dbrepo/helm/dbrepo" \
    --namespace $NAMESPACE \
    --set datadb.rootUser.password=XXXX \
    --set datadb.galera.mariabackup.password=YYYY \
    --set datadb.galera.bootstrap.forceBootstrap=true \
    --set datadb.galera.bootstrap.bootstrapFromNode=M \
    --set datadb.galera.bootstrap.forceSafeToBootstrap=true \
    --set datadb.podManagementPolicy=Parallel
```

Then delete the failing pods (they will be created immediately again).

```shell
kubectl -n $NAMESPACE \
    delete \
    pods \
    -l app.kubernetes.io/name=datadb
```

Then remove the forced bootstrapping.

```shell
helm upgrade \
    --install \
    my-release \
    "oci://registry.datalab.tuwien.ac.at/dbrepo/helm/dbrepo" \
    --namespace $NAMESPACE \
    --set datadb.rootUser.password=XXXX \
    --set datadb.galera.mariabackup.password=YYYY \
    --set datadb.galera.bootstrap.forceBootstrap=true \
    --set datadb.galera.bootstrap.bootstrapFromNode=M \
    --set datadb.galera.bootstrap.forceSafeToBootstrap=false \
    --set datadb.podManagementPolicy=Parallel
```

### Crash Shutdown

This section follows the official [MariaDB 11.3.2 documentation](https://mariadb.com/docs/server/server-usage/backup-and-restore/mariadb-backup/full-backup-and-restore-with-mariadb-backup).
If you need to perform a disaster recovery, start by restoring the backups for each node.

=== "Data Database"

    ```shell
    kubectl -n $NAMESPACE \
        cp \
        backup-data-db-0.tar.gz \
        data-db-0:/bitnami/mariadb/backup.tar.gz
    ```

    Unpack the compressed archive.

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        tar xzfv \
        /bitnami/mariadb/backup.tar.gz && \
        rm -f \
            /bitnami/mariadb/backup.tar.gz
    ```

    Prepare the backup for restore.

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --prepare \ 
        --target-dir=/bitnami/mariadb/backup
    ```

    And ultimately restore the backup.

    ```shell
    kubectl -n $NAMESPACE exec pod/data-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --move-back \ 
        --target-dir=/bitnami/mariadb/backup
    ```

=== "Metadata Database"

    ```shell
    kubectl -n $NAMESPACE \
        cp \
        backup-metadata-db-0.tar.gz \
        data-db-0:/bitnami/mariadb/backup.tar.gz
    ```

    Unpack the compressed archive.

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        tar xzfv \
        /bitnami/mariadb/backup.tar.gz && \
        rm -f \
            /bitnami/mariadb/backup.tar.gz
    ```

    Prepare the backup for restore.

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --prepare \ 
        --target-dir=/bitnami/mariadb/backup
    ```

    And ultimately restore the backup.

    ```shell
    kubectl -n $NAMESPACE exec pod/metadata-db-0 -c mariadb-galera -- \
        mariadb-backup \
        --move-back \ 
        --target-dir=/bitnami/mariadb/backup
    ```

Repeat this for nodes `1` or `2` (or more).

### Troubleshooting

If your restored cluster still fails to reach a quorum (e.g. due to nodes being out of sync and the `SST` mechanism 
fails to transmit the snapshots) you can try to perform
a [manual `SST`](https://mariadb.com/docs/galera-cluster/high-availability/state-snapshot-transfers-ssts-in-galera-cluster/manual-sst-of-galera-cluster-node-with-mariadb-backup).

[^1]: https://mariadb.com/docs/galera-cluster/readme/mariadb-galera-cluster-guide#two-nodes-disappear-from-the-cluster