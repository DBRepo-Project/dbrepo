package at.tuwien.entities.container.image;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
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

    @Column(name = "iid")
    private Long iid;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "iid", insertable = false, updatable = false)
    private ContainerImage image;

    @Column(name = "example", nullable = false)
    private String example;

    @Field(name = "has_time")
    @Column(name = "has_time", nullable = false)
    private Boolean hasTime;

    @Field(name = "database_format")
    @Column(name = "database_format", nullable = false)
    private String databaseFormat;

    @Field(name = "unix_format")
    @Column(name = "unix_format", nullable = false)
    private String unixFormat;

    @CreatedDate
    @Field(name = "created_at")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    @Column(name = "created_at", nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant createdAt;

}
