package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.util.UUID;

@Embeddable
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@EqualsAndHashCode @Builder
public class ReplicaLocation implements Serializable {

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "replica_database_id", columnDefinition = "CHAR(36)")
    private UUID replicaDatabaseId;

    @Column(name = "replica_url", columnDefinition = "TEXT", nullable = false)
    private String url;
}
