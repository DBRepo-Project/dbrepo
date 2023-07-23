package at.tuwien.entities.database.table.columns;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Table(name = "mdb_columns_sets")
public class TableColumnSet {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "columns-sets-sequence")
    @GenericGenerator(name = "columns-sets-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "column_id", referencedColumnName = "id"),
            @JoinColumn(name = "table_id", referencedColumnName = "tID")
    })
    private TableColumn column;

}
