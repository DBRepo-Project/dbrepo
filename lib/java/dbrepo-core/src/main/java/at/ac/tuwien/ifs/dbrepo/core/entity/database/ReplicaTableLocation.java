package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.io.Serializable;
import java.sql.Types;
import java.util.UUID;

@Embeddable
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@EqualsAndHashCode
@Builder
public class ReplicaTableLocation implements Serializable {

    @JdbcTypeCode(Types.VARCHAR)
    @Column(name = "replica_table_id", columnDefinition = "CHAR(36)", nullable = true)
    private UUID replicaTableId;

    @Column(name = "replica_url", columnDefinition = "TEXT", nullable = false)
    private String url;
}