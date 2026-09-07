package at.ac.tuwien.ifs.dbrepo.service.outbox;

public enum ReplicationOutboxOperationType {
    DATABASE_CREATE,
    DATABASE_REPLICA_SYNC,
    TABLE_CREATE,
    TABLE_REPLICA_SYNC,
    VIEW_CREATE,
    DATA_CREATE,
    DATA_UPDATE,
    DATA_DELETE,
    TIMESTAMP_SYNC
}
