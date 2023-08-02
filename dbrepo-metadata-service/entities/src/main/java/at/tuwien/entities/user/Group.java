package at.tuwien.entities.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Immutable
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "keycloak_group")
@NamedQueries({
        @NamedQuery(name = "Group.findDefault", query = "select g from Group g join Realm r on r.id = g.realmId and r.name = 'dbrepo' join RealmDefaultGroup d on d.realmId = r.id and d.groupId = g.id")
})
public class Group {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "REALM_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID realmId;

    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY, mappedBy = "groups")
    private List<User> users;

}
