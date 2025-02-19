package at.tuwien.entities.database.table.constraints.foreignKey;

import at.tuwien.entities.database.table.Table;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@jakarta.persistence.Table(name = "mdb_constraints_foreign_key")
public class ForeignKey {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "fkid", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(updatable = false, nullable = false)
    private String name;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "rtid", referencedColumnName = "id", nullable = false)
    })
    private Table referencedTable;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "foreignKey")
    private List<ForeignKeyReference> references;

    @Column(columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private ReferenceType onUpdate;

    @Column(columnDefinition = "VARCHAR(50)")
    @Enumerated(EnumType.STRING)
    private ReferenceType onDelete;
}
