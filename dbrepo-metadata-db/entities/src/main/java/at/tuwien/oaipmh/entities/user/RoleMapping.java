package at.tuwien.entities.user;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@IdClass(RoleMappingKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_role_mapping", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "ROLE_ID"})
})
public class RoleMapping {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "USER_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ROLE_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID roleId;

}
