package at.ac.tuwien.ifs.dbrepo.metadata.entity;

public enum ReplicationNotificationType {
    DATABASE_PREPARE,
    DATABASE_BOOTSTRAP,
    DATABASE_CREATE,
    TABLE_CREATE,
    TABLE_DELETE,
    VIEW_CREATE
}
