package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
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
        @NamedQuery(name = "DatabaseAccess.findByDatabaseIdAndUserId", query = "select a from DatabaseAccess a where a.hdbid = ?1 and a.huserid = ?2")
})
public class DatabaseAccess {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "user_id", updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID huserid;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User user;

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "database_id", nullable = false, columnDefinition = "VARCHAR(36)")
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
    @Column(nullable = false, name = "access_type", columnDefinition = "enum('READ', 'WRITE_OWN', 'WRITE_ALL')")
    private AccessType type;

}
