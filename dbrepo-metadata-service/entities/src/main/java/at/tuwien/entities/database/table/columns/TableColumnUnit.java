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
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_units", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumnUnit.findAll", query = "select u from TableColumnUnit u order by u.name, u.uri asc"),
        @NamedQuery(name = "TableColumnUnit.findByUri", query = "select u from TableColumnUnit u where u.uri = ?1")
})
public class TableColumnUnit {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "units-sequence")
    @GenericGenerator(name = "units-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "mdb_columns_units",
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false)
            },
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
    private List<TableColumn> columns;
}
