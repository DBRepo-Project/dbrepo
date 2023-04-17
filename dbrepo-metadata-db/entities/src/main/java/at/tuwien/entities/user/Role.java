package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "keycloak_role")
public class Role {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "NAME", nullable = false)
    private String name;

    @Column(name = "REALM_ID", nullable = false)
    private String realmId;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinTable(name = "user_role_mapping",
            joinColumns = {
                    @JoinColumn(name = "ROLE_ID", referencedColumnName = "ID", insertable = false, updatable = false),
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "USER_ID", referencedColumnName = "ID", insertable = false, updatable = false),
            })
    private User user;

}
