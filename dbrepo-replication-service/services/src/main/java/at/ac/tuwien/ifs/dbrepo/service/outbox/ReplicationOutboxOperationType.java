package at.ac.tuwien.ifs.dbrepo.service.outbox;

public enum ReplicationOutboxOperationType {
    DATABASE_PREPARE,
    SUBSET_BACKFILL,
    DATABASE_CREATE,
    DATABASE_REPLICA_SYNC,
    TABLE_CREATE,
    TABLE_DELETE,
    TABLE_REPLICA_SYNC,
    VIEW_CREATE,
    DATA_CREATE,
    DATA_UPDATE,
    DATA_DELETE,
    TIMESTAMP_SYNC,
    HISTORY_SYNC
}
