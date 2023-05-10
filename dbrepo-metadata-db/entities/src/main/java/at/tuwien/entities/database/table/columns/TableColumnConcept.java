package at.tuwien.entities.database.table.columns;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
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
@jakarta.persistence.Table(name = "mdb_concepts")
public class TableColumnConcept {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(name = "name", nullable = false)
    private String name;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "mdb_columns_concepts",
            joinColumns = @JoinColumn(name = "uri", referencedColumnName = "uri", insertable = false, updatable = false),
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "tid", referencedColumnName = "tid", insertable = false, updatable = false),
                    @JoinColumn(name = "cdbid", referencedColumnName = "cdbid", insertable = false, updatable = false)
            })
    private List<TableColumn> columns;

    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @CreatedDate
    private Instant created;
}
