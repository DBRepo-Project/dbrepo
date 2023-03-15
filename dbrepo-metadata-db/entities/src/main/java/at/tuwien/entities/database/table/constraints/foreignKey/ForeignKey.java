package at.tuwien.entities.database.table.constraints.foreignKey;

import at.tuwien.entities.database.table.Table;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_constraints_foreign_key")
public class ForeignKey {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "foreign-key-sequence")
    @GenericGenerator(name = "foreign-key-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long fkid;

    @Column
    private Long tid;

    @Column
    private Long tdbid;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "tdbid", referencedColumnName = "tdbid", nullable = false, insertable = false, updatable = false)
    })
    private Table table;

    @Column
    private Long rtid;

    @Column
    private Long rtdbid;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "rtid", referencedColumnName = "id", nullable = false, insertable = false, updatable = false),
            @JoinColumn(name = "rtdbid", referencedColumnName = "tdbid", nullable = false, insertable = false, updatable = false)
    })
    private Table referencedTable;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "foreignKey")
    private List<ForeignKeyReference> references = new java.util.ArrayList<>();

    @Column
    private ReferenceType onUpdate;

    @Column
    private ReferenceType onDelete;
}
