package at.tuwien.entities.user;

import at.tuwien.entities.container.Container;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_users")
@NamedNativeQueries({
        @NamedNativeQuery(name = "User.findAll",
                query = "SELECT e.* FROM `keycloak`.`REALM` r JOIN `keycloak`.`USER_ENTITY` e ON r.`ID` = e.`REALM_ID` WHERE r.`NAME` = 'dbrepo' AND e.`USERNAME` != 'system'",
                resultClass = User.class),
        @NamedNativeQuery(name = "User.findByUsername",
                query = "SELECT e.* FROM `keycloak`.`REALM` r JOIN `keycloak`.`USER_ENTITY` e ON r.`ID` = e.`REALM_ID` WHERE r.`NAME` = 'dbrepo' AND e.`USERNAME` = ?",
                resultClass = User.class)
})
public class User {


    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "users-sequence")
    @GenericGenerator(name = "users-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private String id;

    @Column(unique = true, nullable = false)
    private String username;

    @Column(name = "first_name")
    private String firstname;

    @Column(name = "last_name")
    private String lastname;

    @Column(name = "preceding_titles")
    private String titlesBefore;

    @Column(name = "postpositioned_title")
    private String titlesAfter;

    @Column(name = "main_email", unique = true, nullable = false)
    private String email;

    @Column
    private String affiliation;

    @Column
    private String orcid;

    @Column(nullable = false)
    private Boolean themeDark;

    @Column(name = "main_email_verified", nullable = false)
    private Boolean emailVerified;

    @ToString.Exclude
    @Column(nullable = false)
    private String password;

    @ToString.Exclude
    @Column(nullable = false)
    private String databasePassword;

    @Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "creator")
    private List<Container> containers;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant created;

    @LastModifiedDate
    @Column(name = "last_modified")
    private Instant lastModified;

}
