package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.nio.charset.StandardCharsets;
import java.util.UUID;

@Entity
@Getter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Table(name = "mdb_replication_creations", uniqueConstraints =
        @UniqueConstraint(columnNames = {"kind", "parent_id", "physical_name"}))
public class ReplicationCreation {
    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(length = 36)
    private UUID id;

    @Column(nullable = false, length = 16)
    private String kind;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "parent_id", nullable = false, length = 36)
    private UUID parentId;

    @Column(name = "physical_name", nullable = false, length = 64)
    private String physicalName;

    @Column(name = "payload_hash", nullable = false, length = 64)
    private String payloadHash;

    public static UUID localId(String kind, UUID parentId, String origin, UUID creationId) {
        if (origin == null || origin.isBlank() || creationId == null || parentId == null) {
            throw new IllegalArgumentException("Replication creation requires an origin, parent and creation id");
        }
        return UUID.nameUUIDFromBytes((kind + "\n" + parentId + "\n" + creationId + "\n"
                + origin.trim().replaceAll("/+$", "")).getBytes(StandardCharsets.UTF_8));
    }

    public static String databaseName(UUID id) {
        return "replica_" + id.toString().replace("-", "");
    }
}
