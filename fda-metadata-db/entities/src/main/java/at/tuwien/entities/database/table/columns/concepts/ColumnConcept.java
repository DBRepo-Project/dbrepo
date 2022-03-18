package at.tuwien.entities.database.table.columns.concepts;


import at.tuwien.entities.database.table.columns.TableColumnKey;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@IdClass(TableColumnKey.class)
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_columns_concepts")
public class ColumnConcept {

    @Id
    @EqualsAndHashCode.Include
    @Column(name = "cid", nullable = false)
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long tid;

    @Id
    @EqualsAndHashCode.Include
    private Long cdbid;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name="uri")
    private Concept concept;
}
