package at.tuwien.entities.container.image;

import at.tuwien.entities.container.Container;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@Table(name = "mdb_images", uniqueConstraints = @UniqueConstraint(columnNames = {"name", "version"}))
@NamedQueries({
        @NamedQuery(name = "ContainerImage.findAll", query = "select i from ContainerImage i order by i.id asc")
})
public class ContainerImage {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String registry;

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

    @Column(nullable = false, unique = true, columnDefinition = "BOOLEAN DEFAULT FALSE")
    private Boolean isDefault = false;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "image")
    private List<Container> containers;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "image")
    private List<DataType> dataTypes;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "image")
    private List<Operator> operators;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
