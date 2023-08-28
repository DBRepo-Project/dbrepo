package at.tuwien.entities.user;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@IdClass(GroupMembershipKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_group_membership", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"USER_ID", "GROUP_ID"})
})
public class GroupMembership {

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "USER_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID userId;

    @Id
    @EqualsAndHashCode.Include
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "GROUP_ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID groupId;

}
