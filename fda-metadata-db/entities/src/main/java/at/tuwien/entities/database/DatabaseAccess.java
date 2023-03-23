package at.tuwien.entities.database;

import at.tuwien.entities.user.User;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(DatabaseAccessKey.class)
@EntityListeners(AuditingEntityListener.class)
@javax.persistence.Table(name = "mdb_have_access")
public class DatabaseAccess {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "user_id", updatable = false)
    private String huserid;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "database_id", updatable = false)
    private Long hdbid;

    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.DETACH, CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "userid", updatable = false, insertable = false)
    })
    private User user;

    @Column(nullable = false, name = "access_type", columnDefinition = "enum('READ', 'WRITE_OWN', 'WRITE_ALL')")
    @Enumerated(EnumType.STRING)
    private AccessType type;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

}
