package at.tuwien.entities.user;

import at.tuwien.entities.database.DatabaseAccess;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Log4j2
@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_users")
public class User {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column
    private String firstname;

    @Column
    private String lastname;

    @Column(nullable = false)
    private String email;

    @Column
    private String orcid;

    @Column
    private String affiliation;

    @Column
    private String language;

    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private List<DatabaseAccess> accesses;

    @Column(nullable = false)
    private String theme;

    @Column(name = "mariadb_password", nullable = false)
    private String mariadbPassword;

}
