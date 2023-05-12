package at.tuwien.entities.database.table.constraints.unique;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@jakarta.persistence.Table(name = "mdb_constraints_unique")
public class Unique {

    @Id
    @GeneratedValue(generator = "constraints-unique-sequence")
    @GenericGenerator(name = "constraints-unique-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    @EqualsAndHashCode.Include
    private Long uid;

    @Column
    private Long tid;

    @Column
    private Long tdbid;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "tdbid", referencedColumnName = "tdbid", insertable = false, updatable = false)
    })
    private Table table;

    @ManyToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "mdb_constraints_unique_columns",
            joinColumns = {
                    @JoinColumn(name = "uid", referencedColumnName = "uid", insertable = false, updatable = false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "ctid", referencedColumnName = "tid", insertable = false, updatable = false),
                    @JoinColumn(name = "ctdbid", referencedColumnName = "cdbid", insertable = false, updatable = false)
            }
    )
    private List<TableColumn> columns;
}
