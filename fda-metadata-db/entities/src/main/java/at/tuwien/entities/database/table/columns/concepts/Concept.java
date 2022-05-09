package at.tuwien.entities.database.table.columns.concepts;

import at.tuwien.entities.database.table.columns.TableColumn;
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

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "mdb_columns_concepts",
            joinColumns = @JoinColumn(name = "uri"),
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "tid", referencedColumnName = "tid", insertable = false, updatable = false),
                    @JoinColumn(name = "cdbid", referencedColumnName = "cdbid", insertable = false, updatable = false)
            })
    private List<TableColumn> columns;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;
}
