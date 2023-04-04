package at.tuwien.entities.database.table.columns;

import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
@javax.persistence.Table(name = "mdb_units")
public class TableColumnUnit {

    @Id
    @EqualsAndHashCode.Include
    @Column(nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(name = "name", nullable = false)
    private String name;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY)
    @JoinTable(name = "mdb_columns_units",
            joinColumns = @JoinColumn(name = "uri", referencedColumnName = "uri", insertable = false, updatable = false),
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
