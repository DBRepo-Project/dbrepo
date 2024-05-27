package at.tuwien.entities.database.table.constraints.foreignKey;

import at.tuwien.entities.database.table.Table;
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
@jakarta.persistence.Table(name = "mdb_constraints_foreign_key")
public class ForeignKey {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "foreign-key-sequence")
    @GenericGenerator(name = "foreign-key-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long fkid;

    @Column(updatable = false, nullable = false)
    private String name;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @ToString.Exclude
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
