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
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "credential")
public class Credential {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "credential-uuid")
    @GenericGenerator(name = "credential-uuid", strategy = "org.hibernate.id.UUIDGenerator")
    @Column(name = "ID", nullable = false, columnDefinition = "VARCHAR(36)")
    private String id;

    @Column(nullable = false)
    private String type;

    @Column(name = "user_id", nullable = false)
    private String userId;

    @Column(nullable = false)
    private Long createdDate;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String secretData;

    @Column(nullable = false, columnDefinition = "LONGTEXT")
    private String credentialData;

    @Column(nullable = false)
    private Integer priority;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "user_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private User user;

}
