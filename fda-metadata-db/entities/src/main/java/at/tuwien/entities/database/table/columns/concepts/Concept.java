package at.tuwien.entities.database.table.columns.concepts;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_concepts")
public class Concept {

    @Id
    @Column(name = "URI", nullable = false)
    private String uri;

    @Column(name = "name", nullable = false)
    private String name;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "concept")
    private List<ColumnConcept> columnConcept;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;
}
