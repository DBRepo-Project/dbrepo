package at.tuwien.entities.user;

import at.tuwien.entities.database.DatabaseAccess;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

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
@NamedQueries({
        @NamedQuery(name = "User.findAllInternal", query = "select distinct u from User u where u.isInternal = true")
})
public class User {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "keycloak_id", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID keycloakId;

    @Column(nullable = false)
    private String username;

    @Column
    private String firstname;

    @Column
    private String lastname;

    @Column
    private String orcid;

    @Column
    private String affiliation;

    @Column
    private String language;

    @OneToMany(fetch = FetchType.LAZY)
    @EqualsAndHashCode.Exclude
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private List<DatabaseAccess> accesses;

    @Column(nullable = false)
    private String theme;

    @Column(name = "mariadb_password", nullable = false)
    private String mariadbPassword;

    @Column(name = "is_internal", nullable = false, updatable = false)
    private Boolean isInternal;

}
