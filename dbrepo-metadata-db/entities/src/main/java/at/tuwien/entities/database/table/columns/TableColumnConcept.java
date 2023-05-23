package at.tuwien.entities.database.table.columns;

import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.net.URI;
import java.sql.Types;
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
@jakarta.persistence.Table(name = "mdb_concepts")
@NamedQueries({
        @NamedQuery(name = "TableColumnConcept.findById", query = "select c from TableColumnConcept c where c.uri = ?1")
})
public class TableColumnConcept {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(name = "name")
    private String name;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @CreatedDate
    private Instant created;
}
