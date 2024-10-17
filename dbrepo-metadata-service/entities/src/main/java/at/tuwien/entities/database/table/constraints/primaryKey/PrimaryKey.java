package at.tuwien.entities.database.table.constraints.primaryKey;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@jakarta.persistence.Table(name = "mdb_constraints_primary_key")
public class PrimaryKey {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "foreign-key-sequence")
    @GenericGenerator(name = "foreign-key-sequence", strategy = "increment")
    @Column(name = "pkid", updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;
}
