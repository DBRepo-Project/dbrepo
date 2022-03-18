package at.tuwien.entities.database.table.columns.concepts;

import at.tuwien.entities.database.table.columns.TableColumnKey;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

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

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;
}
