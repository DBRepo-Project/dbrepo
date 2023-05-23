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
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@jakarta.persistence.Table(name = "mdb_units")
@NamedQueries({
        @NamedQuery(name = "TableColumnUnit.findById", query = "select u from TableColumnUnit u where u.uri = ?1")
})
public class TableColumnUnit {

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
