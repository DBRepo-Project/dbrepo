package at.ac.tuwien.ifs.dbrepo.entity;

import jakarta.persistence.*;
import lombok.*;

import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@jakarta.persistence.Table(name = "tuple_replication_timestamps")
@IdClass(TupleReplicationTimestamp.TupleReplicationTimestampId.class)
public class TupleReplicationTimestamp implements Serializable {

    @Id
    @Column(name = "site_url", nullable = false, columnDefinition = "TEXT")
    private String siteUrl;

    @Id
    @Column(name = "replication_id", nullable = false, columnDefinition = "VARCHAR(255)")
    private String replicationId;

    @Column(name = "database_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID databaseId;

    @Column(name = "table_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID tableId;

    @Column(name = "row_start", nullable = false, columnDefinition = "TIMESTAMP")
    private Instant rowStart;

    @Column(name = "row_end", columnDefinition = "TIMESTAMP")
    private Instant rowEnd;

    // Composite primary key class
    @Data
    @NoArgsConstructor
    @AllArgsConstructor
    @EqualsAndHashCode
    public static class TupleReplicationTimestampId implements Serializable {
        private String siteUrl;
        private String replicationId;
    }
}
