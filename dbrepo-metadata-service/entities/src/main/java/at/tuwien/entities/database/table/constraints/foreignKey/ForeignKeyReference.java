package at.tuwien.entities.database.table.constraints.foreignKey;

import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_constraints_foreign_key_reference")
public class ForeignKeyReference {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "foreign-key-reference-sequence")
    @GenericGenerator(name = "foreign-key-reference-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "fkid", referencedColumnName = "fkid", nullable = false)
    private ForeignKey foreignKey;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumns({
            @JoinColumn(name = "rcid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn referencedColumn;

}
