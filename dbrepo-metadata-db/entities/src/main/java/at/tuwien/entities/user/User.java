package at.tuwien.entities.user;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_entity", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"REALM_ID", "EMAIL"}),
        @UniqueConstraint(columnNames = {"REALM_ID", "USERNAME"})
})
@NamedQueries({
        @NamedQuery(name = "User.findAll", query = "select u from User u join Realm r on r.name = 'dbrepo'"),
        @NamedQuery(name = "User.findById", query = "select u from User u join Realm r on r.name = 'dbrepo' and u.id = ?1"),
        @NamedQuery(name = "User.findByUsername", query = "select u from User u join Realm r on r.name = 'dbrepo' and u.username = ?1")
})
public class User {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "user-uuid")
    @GenericGenerator(name = "user-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(nullable = false)
    private String username;

    @Column(name = "FIRST_NAME")
    private String firstname;

    @Column(name = "LAST_NAME")
    private String lastname;

    @Column(name = "REALM_ID")
    private String realmId;

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
    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.ALL, mappedBy = "user")
    private List<Role> roles;

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
    private List<Container> containers;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "owner")
    private List<Database> databases;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "creator")
    private List<Identifier> identifiers;

}
