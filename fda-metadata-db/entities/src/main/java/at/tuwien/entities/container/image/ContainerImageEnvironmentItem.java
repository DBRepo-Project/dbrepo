package at.tuwien.entities.container.image;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(ContainerImageEnvironmentItemKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_images_environment_item")
public class ContainerImageEnvironmentItem {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    public Long id;

    @Id
    @EqualsAndHashCode.Include
    public Long iid;

    @Column(nullable = false)
    private String key;

    @Column(nullable = false)
    private String value;

    @Column(nullable = false, name = "etype", columnDefinition = "enum('USERNAME', 'PASSWORD', 'PRIVILEGED_USERNAME', 'PRIVILEGED_PASSWORD')")
    @Enumerated(EnumType.STRING)
    private ContainerImageEnvironmentItemType type;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "iid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private ContainerImage image;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}

