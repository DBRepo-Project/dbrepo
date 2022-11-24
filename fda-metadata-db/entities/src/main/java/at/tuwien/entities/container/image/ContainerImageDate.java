package at.tuwien.entities.container.image;

import at.tuwien.entities.database.table.columns.TableColumn;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
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
@Table(name = "mdb_images_date", uniqueConstraints = @UniqueConstraint(columnNames = {"database_format"}))
public class ContainerImageDate {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "iid")
    private Long iid;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "iid", insertable = false, updatable = false)
    private ContainerImage image;

    @Column(name = "example", nullable = false)
    private String example;

    @Column(name = "has_time", nullable = false)
    private Boolean hasTime;

    @Column(name = "database_format", nullable = false)
    private String databaseFormat;

    @Column(name = "unix_format", nullable = false)
    private String unixFormat;

    @CreatedDate
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

}
