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
@Table(name = "mdb_columns_enums")
public class TableColumnEnum {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "columns-enums-sequence")
    @GenericGenerator(name = "columns-enums-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    private String value;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "column_id", referencedColumnName = "id")
    })
    private TableColumn column;

}
