package at.tuwien.entities.container.image;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_image_operators")
public class Operator {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "image-type-sequence")
    @GenericGenerator(name = "image-type-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    public Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "value", nullable = false, unique = true)
    private String value;

    @Column(nullable = false)
    private String documentation;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "image_id", referencedColumnName = "id")
    })
    private ContainerImage image;

}
