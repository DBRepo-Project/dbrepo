package at.tuwien.entities.database.table.columns;

import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

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
@Table(name = "mdb_concepts", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumnConcept.findAll", query = "select c from TableColumnConcept c order by c.name, c.uri asc"),
        @NamedQuery(name = "TableColumnConcept.findByUri", query = "select c from TableColumnConcept c where c.uri = ?1")
})
public class TableColumnConcept {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "concepts-sequence")
    @GenericGenerator(name = "concepts-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(name = "mdb_columns_concepts",
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false)
            },
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
    private List<TableColumn> columns;
}
