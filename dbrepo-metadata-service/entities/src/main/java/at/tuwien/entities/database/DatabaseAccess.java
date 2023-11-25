package at.tuwien.entities.database;

import at.tuwien.converters.AccessTypeConverter;
import at.tuwien.entities.user.User;
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
@NamedQueries({
        @NamedQuery(name ="DatabaseAccess.findByDatabaseId", query = "select a from DatabaseAccess a where a.hdbid = ?1"),
        @NamedQuery(name ="DatabaseAccess.findByDatabaseIdAndUserId", query = "select a from DatabaseAccess a where a.hdbid = ?1 and a.huserid = ?2")
})
public class DatabaseAccess {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "user_id", updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID huserid;

    @org.springframework.data.annotation.Transient
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User user;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "database_id", updatable = false)
    private Long hdbid;

    @org.springframework.data.annotation.Transient
    @ManyToOne(cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "database_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @Column(nullable = false, name = "access_type", columnDefinition = "enum('read', 'write_own', 'write_all')")
    @Convert(converter = AccessTypeConverter.class)
    private AccessType type;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @CreatedDate
    private Instant created;

}
