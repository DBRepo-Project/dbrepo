package at.tuwien.entities.user;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Immutable;
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
@Table(name = "user_entity")
public class User {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "user-uuid")
    @GenericGenerator(name = "user-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "first_name")
    private String firstname;

    @Column(name = "last_name")
    private String lastname;

    @Column(name = "realm_id")
    private String realmId;

    @Column(unique = true, nullable = false)
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
