package at.tuwien.entities.container.image;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@Table(name = "mdb_image_operators")
public class Operator {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    public Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "value", nullable = false, unique = true)
    private String value;

    @Column(nullable = false)
    private String documentation;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "image_id", referencedColumnName = "id")
    })
    private ContainerImage image;

}
