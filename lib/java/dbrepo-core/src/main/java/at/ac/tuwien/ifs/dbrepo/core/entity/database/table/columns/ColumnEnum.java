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
@jakarta.persistence.Table(name = "mdb_columns_enums", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"value"})
})
public class ColumnEnum {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
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
