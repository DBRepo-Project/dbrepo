package at.tuwien.entities.database.table.columns.concepts;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.validation.constraints.NotNull;
import java.io.Serializable;
import java.net.URI;

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
    @Column(name = "cid", nullable = false)
    private Long cid;

    @Id
    @EqualsAndHashCode.Include
    private Long tid;

    @Id
    @EqualsAndHashCode.Include
    private Long cdbid;

    @NotNull
    private URI uri;
}
