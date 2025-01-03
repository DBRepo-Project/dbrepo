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
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_image_types")
public class DataType {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    public Long id;

    @Column(name = "display_name", nullable = false)
    private String displayName;

    @Column(name = "value", nullable = false, unique = true)
    private String value;

    @Column(name = "size_min", nullable = false)
    private Integer sizeMin;

    @Column(name = "size_max")
    private Integer sizeMax;

    @Column(name = "size_default")
    private Integer sizeDefault;

    @Column(name = "size_required", nullable = false)
    private Boolean sizeRequired;

    @Column(name = "d_min")
    private Integer dMin;

    @Column(name = "d_max")
    private Integer dMax;

    @Column(name = "d_default")
    private Integer dDefault;

    @Column(name = "d_required", nullable = false)
    private Boolean dRequired;

    @Column(nullable = false)
    private String documentation;

    @Column(name = "type_hint")
    private String typeHint;

    @Column(name = "data_hint")
    private String dataHint;

    @Column(name = "is_quoted", nullable = false)
    private Boolean quoted;

    @Column(name = "is_buildable", nullable = false)
    private Boolean buildable;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "image_id", referencedColumnName = "id")
    })
    private ContainerImage image;

}
