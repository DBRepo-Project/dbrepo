package at.tuwien.entities.container;

import at.tuwien.entities.container.image.ContainerImage;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
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
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_containers")
public class Container {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "containers-sequence")
    @GenericGenerator(name = "containers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID")
    })
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "ownedBy", referencedColumnName = "ID")
    })
    private User owner;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(nullable = false)
    private String hash;

    @Column(name = "image_id", nullable = false, updatable = false)
    private Long imageId;

    @Column
    private Integer port;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "image_id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private ContainerImage image;

    @Column
    private String ipAddress;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
