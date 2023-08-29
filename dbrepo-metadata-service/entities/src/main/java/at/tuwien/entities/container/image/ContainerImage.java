package at.tuwien.entities.container.image;

import at.tuwien.entities.container.Container;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_images", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
public class ContainerImage {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "images-sequence")
    @GenericGenerator(name = "images-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    public Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String version;

    @Column(nullable = false)
    private String driverClass;

    @Column(nullable = false)
    private String dialect;

    @Column(nullable = false)
    private String jdbcMethod;

    @Column(nullable = false)
    private Integer defaultPort;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "image")
    private List<ContainerImageDate> dateFormats;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "image")
    private List<Container> containers;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
