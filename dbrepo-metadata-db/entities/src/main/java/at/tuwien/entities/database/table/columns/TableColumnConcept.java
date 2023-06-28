package at.tuwien.entities.database.table.columns;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
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
@jakarta.persistence.Table(name = "mdb_concepts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumnConcept.findById", query = "select c from TableColumnConcept c where c.uri = ?1")
})
@Document(indexName = "concept")
public class TableColumnConcept {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "concepts-sequence")
    @GenericGenerator(name = "concepts-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @EqualsAndHashCode.Include
    @Column(nullable = false, unique = true, columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @CreatedDate
    private Instant created;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY)
    @org.springframework.data.annotation.Transient
    @JoinTable(name = "mdb_columns_concepts",
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "tid", referencedColumnName = "tid", insertable = false, updatable = false),
                    @JoinColumn(name = "cdbid", referencedColumnName = "cdbid", insertable = false, updatable = false)
            },
            joinColumns = @JoinColumn(name = "uri", referencedColumnName = "uri"))
    private List<TableColumn> columns;
}
