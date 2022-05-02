package at.tuwien.entities.database.table.columns.concepts;


import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_columns_concepts")
public class ColumnConcept implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "concept-sequence")
    @GenericGenerator(
            name = "concept-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_columns_concepts_seq")
    )
    private Long id;

    @NotNull
    @Column(name = "cid", nullable = false)
    private Long cid;

    @NotNull
    private Long tid;

    @NotNull
    private Long cdbid;

    @OneToMany(fetch = FetchType.EAGER, cascade = CascadeType.MERGE, mappedBy = "columnConcept")
    private List<TableColumn> tableColumn;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.MERGE)
    @JoinColumn(name = "uri")
    private Concept concept;
}
