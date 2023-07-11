package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
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
@Table(name = "keycloak_role")
@NamedQueries({
        @NamedQuery(name = "Role.findDefault", query = "select r from Role r join Realm rr on rr.name = 'dbrepo' and rr.id = r.realmId and rr.defaultRole = r.id")
})
public class Role {

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

}
