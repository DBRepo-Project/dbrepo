package at.tuwien.entities.database.table.columns.concepts;

import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
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
    @EqualsAndHashCode.Include
    @GenericGenerator(name = "native", strategy = "native")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    private Long id;

    @Column(name = "URI", nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(name = "name", nullable = false)
    private String name;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "mdb_columns_concepts",
            joinColumns = @JoinColumn(name = "concept_id", referencedColumnName = "id", insertable = false, updatable = false),
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
