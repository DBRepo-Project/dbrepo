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
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_users")
public class User {

    @Id
    @EqualsAndHashCode.Include
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

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private List<DatabaseAccess> accesses;

    @Column(nullable = false)
    private String theme;

    @ToString.Exclude
    @Column(name = "mariadb_password", nullable = false)
    private String mariadbPassword;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (o instanceof Principal principal) {
            final boolean result = this.getUsername().equals(principal.getName());
            log.trace("check if username {} equals principal name {}: {}", username, principal.getName(), result);
            return result;
        }
        if (!(o instanceof User other)) {
            return false;
        }
        final boolean result = this.getId().equals(other.getId());
        log.trace("check if id {} equals other id {}: {}", id, other.getId(), result);
        return result;
    }

    @Override
    public int hashCode() {
        int hash = 7;
        hash = 31 * hash + (id == null ? 0 : id.hashCode());
        hash = 31 * hash + (username == null ? 0 : username.hashCode());
        hash = 31 * hash + (firstname == null ? 0 : firstname.hashCode());
        hash = 31 * hash + (lastname == null ? 0 : lastname.hashCode());
        hash = 31 * hash + (email == null ? 0 : email.hashCode());
        hash = 31 * hash + (orcid == null ? 0 : orcid.hashCode());
        hash = 31 * hash + (affiliation == null ? 0 : affiliation.hashCode());
        hash = 31 * hash + (language == null ? 0 : language.hashCode());
        hash = 31 * hash + (accesses == null ? 0 : accesses.hashCode());
        hash = 31 * hash + (theme == null ? 0 : theme.hashCode());
        hash = 31 * hash + (mariadbPassword == null ? 0 : mariadbPassword.hashCode());
        return hash;
    }

}
