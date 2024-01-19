package at.tuwien.entities.container;

import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import com.fasterxml.jackson.annotation.JsonFormat;
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
@Table(name = "mdb_containers")
@NamedQueries({
        @NamedQuery(name = "Container.findDefaultTimestampFormat", query = "select d from ContainerImageDate d where d.hasTime = true order by d.id limit 1"),
        @NamedQuery(name = "Container.findDefaultDateFormat", query = "select d from ContainerImageDate d where d.hasTime = false order by d.id limit 1"),
})
public class Container {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "containers-sequence")
    @GenericGenerator(name = "containers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(nullable = false)
    private String host;

    @Column
    private Integer port;

    @Column(nullable = false)
    private String sidecarHost;

    @Column(nullable = false)
    private Integer sidecarPort;

    @Column
    private String uiHost;

    @Column
    private Integer uiPort;

    @Column
    private String uiAdditionalFlags;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<Database> databases;

    @ManyToOne(fetch = FetchType.LAZY, optional = false, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinColumns({
            @JoinColumn(name = "image_id", referencedColumnName = "id")
    })
    private ContainerImage image;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @Column
    private String privilegedUsername;

    @Column
    private String privilegedPassword;

}
