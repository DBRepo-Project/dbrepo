package at.ac.tuwien.ifs.dbrepo.core.entity.container.image;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@EqualsAndHashCode
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_image_operators")
public class Operator {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
