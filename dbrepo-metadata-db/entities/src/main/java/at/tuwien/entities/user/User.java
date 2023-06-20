package at.tuwien.entities.user;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;
import org.springframework.security.core.Authentication;

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
@Table(name = "USER_ENTITY", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"REALM_ID", "EMAIL"}),
        @UniqueConstraint(columnNames = {"REALM_ID", "USERNAME"})
})
@NamedQueries({
        @NamedQuery(name = "User.findAll", query = "select u from User u join Realm r on r.name = 'dbrepo' and u.enabled = true"),
        @NamedQuery(name = "User.findById", query = "select u from User u join Realm r on r.name = 'dbrepo' and u.id = ?1 and u.enabled = true"),
        @NamedQuery(name = "User.findByUsername", query = "select u from User u join Realm r on r.name = 'dbrepo' and u.username = ?1 and u.enabled = true")
})
public class User {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String username;

    @Column(name = "FIRST_NAME")
    private String firstname;

    @Column(name = "LAST_NAME")
    private String lastname;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "REALM_ID", columnDefinition = "VARCHAR(36)")
    private UUID realmId;

    @Column(nullable = false)
    private String email;

    @Column(nullable = false)
    private Boolean emailVerified;

    @Column(nullable = false)
    private Boolean enabled;

    @Column
    private Long createdTimestamp;

    @Transient
    @ToString.Exclude
    @Column(nullable = false)
    private String databasePassword;

    @Column(nullable = false)
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    private List<UserAttribute> attributes;

    @Column(nullable = false)
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "user")
    private List<Credential> credentials;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "owner")
    private List<Database> databases;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "creator")
    private List<Identifier> identifiers;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_role_mapping",
            joinColumns = {
                    @JoinColumn(name = "USER_ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false),
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "ROLE_ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false),
            })
    private List<Role> roles;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "user_group_membership",
            joinColumns = {
                    @JoinColumn(name = "USER_ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false),
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "GROUP_ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false),
            })
    private List<Group> groups;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "creator")
    private List<at.tuwien.entities.database.table.Table> tables;

    /**
     * Compares if the user instance equals with another instance by the principal.
     *
     * @param principal The user principal.
     * @return True if the user are equal, false otherwise.
     */
    public boolean equalsPrincipal(Principal principal) {
        if (principal == null) {
            return false;
        }
        return this.username.equals(principal.getName());
    }

    /**
     * Compares the user principal and checks if a certain role is present.
     *
     * @param principal The user principal.
     * @param role      The role.
     * @return True if the role is present, false otherwise.
     */
    public static boolean hasRole(Principal principal, String role) {
        if (principal == null || role == null) {
            return false;
        }
        final Authentication authentication = (Authentication) principal;
        return authentication.getAuthorities()
                .stream()
                .anyMatch(a -> a.getAuthority().equals(role));
    }

}
