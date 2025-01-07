package at.tuwien.entities.database.table.constraints.unique;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@jakarta.persistence.Table(name = "mdb_constraints_unique")
public class Unique {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(name = "uid", updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String name;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id")
    })
    private Table table;

    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
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
