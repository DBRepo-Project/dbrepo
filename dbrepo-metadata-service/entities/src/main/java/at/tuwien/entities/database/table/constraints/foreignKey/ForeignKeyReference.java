package at.tuwien.entities.database.table.constraints.foreignKey;

import at.tuwien.entities.database.table.columns.TableColumn;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@Table(name = "mdb_constraints_foreign_key_reference", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"fkid", "cid", "rcid"})
})
public class ForeignKeyReference {

    @Id
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumn(name = "fkid", referencedColumnName = "fkid", nullable = false)
    private ForeignKey foreignKey;

    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;

    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "rcid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn referencedColumn;

}
