package at.tuwien.entities.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Immutable;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Immutable
@IdClass(RealmDefaultGroupKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "realm_default_groups")
public class RealmDefaultGroup {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "GROUP_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID groupId;

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "REALM_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID realmId;

}
