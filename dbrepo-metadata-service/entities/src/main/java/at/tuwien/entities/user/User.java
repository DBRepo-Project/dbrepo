package at.tuwien.entities.user;

import at.tuwien.entities.database.DatabaseAccess;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.security.Principal;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_users")
@NamedQueries({
        @NamedQuery(name = "User.findByUsername", query = "select u from User u where u.username = ?1")
})
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
            return this.getUsername().equals(principal.getName());
        }
        if (!(o instanceof User other)) {
            return false;
        }
        return this.getId().equals(other.getId());
    }

}
