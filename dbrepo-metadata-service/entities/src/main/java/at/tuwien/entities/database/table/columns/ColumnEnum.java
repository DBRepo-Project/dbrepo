package at.tuwien.entities.database.table.columns;

import jakarta.persistence.*;
import lombok.*;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@jakarta.persistence.Table(name = "mdb_columns_enums", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"value"})
})
public class ColumnEnum {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "column_id", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;

    @Column(columnDefinition = "VARCHAR(255)")
    private String value;
}
