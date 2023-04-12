package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@IdClass(RoleMappingKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "user_role_mapping")
public class RoleMapping {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "USER_ID", nullable = false)
    private String userId;

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ROLE_ID", nullable = false)
    private String roleId;

}
