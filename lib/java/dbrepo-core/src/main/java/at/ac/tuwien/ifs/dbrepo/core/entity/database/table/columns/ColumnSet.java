package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;

import java.util.UUID;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@Table(name = "mdb_columns_sets", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"value"})
})
public class ColumnSet {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "column_id", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;

    @Column(columnDefinition = "VARCHAR(255)")
    private String value;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
