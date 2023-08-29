package at.tuwien.entities.database;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(DatabaseAccessKey.class)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_have_access")
public class DatabaseAccess {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "user_id", updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID huserid;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "database_id", updatable = false)
    private Long hdbid;

    @Column(nullable = false, name = "access_type", columnDefinition = "enum('READ', 'WRITE_OWN', 'WRITE_ALL')")
    @Enumerated(EnumType.STRING)
    private AccessType type;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @CreatedDate
    private Instant created;

}
