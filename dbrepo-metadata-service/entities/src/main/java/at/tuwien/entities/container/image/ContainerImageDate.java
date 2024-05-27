package at.tuwien.entities.container.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

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
    @GeneratedValue(generator = "dates-sequence")
    @GenericGenerator(name = "dates-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(name = "iid")
    private Long iid;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "iid", insertable = false, updatable = false)
    })
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
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant createdAt;

}
