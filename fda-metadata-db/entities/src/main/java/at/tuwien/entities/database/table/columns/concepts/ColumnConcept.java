package at.tuwien.entities.database.table.columns.concepts;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;

/**
 * Join Table
 */
@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@IdClass(ColumnConceptKey.class)
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_columns_concepts")
public class ColumnConcept implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long cid;

    @Id
    @EqualsAndHashCode.Include
    private Long tid;

    @Id
    @EqualsAndHashCode.Include
    private Long cdbid;

    @EqualsAndHashCode.Include
    @Column(name = "concept_id")
    private Long conceptId;
}
