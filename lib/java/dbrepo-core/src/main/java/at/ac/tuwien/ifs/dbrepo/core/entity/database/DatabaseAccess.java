package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@IdClass(DatabaseAccessKey.class)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_have_access")
@NamedQueries({
        @NamedQuery(name = "DatabaseAccess.findByDatabaseId", query = "select a from DatabaseAccess a where a.hdbid = ?1"),
        @NamedQuery(name = "DatabaseAccess.findByDatabaseIdAndUserId", query = "select a from DatabaseAccess a where a.hdbid = ?1 and a.username = ?2")
})
public class DatabaseAccess {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private String username;

    @Id
    @Column(name = "database_id", updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID hdbid;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "database_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(nullable = false, name = "access_type", columnDefinition = "enum('READ', 'WRITE_OWN', 'WRITE_ALL')")
    private AccessType type;

}
