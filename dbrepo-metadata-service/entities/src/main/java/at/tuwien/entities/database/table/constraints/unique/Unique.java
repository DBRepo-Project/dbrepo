package at.tuwien.entities.database.table.constraints.unique;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

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
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "constraints-unique-sequence")
    @GenericGenerator(name = "constraints-unique-sequence", strategy = "increment")
    @Column(name = "uid", updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String name;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id")
    })
    private Table table;

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(
            name = "mdb_constraints_unique_columns",
            joinColumns = {
                    @JoinColumn(name = "uid", referencedColumnName = "uid")
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id")
            }
    )
    private List<TableColumn> columns;
}
