package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumn;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_view_columns", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cid", "vid"})
})
public class ViewColumn implements Comparable<ViewColumn> {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "view-columns-sequence")
    @GenericGenerator(name = "view-columns-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private String alias;

    @Column(nullable = false)
    private Integer ordinalPosition;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "vid", referencedColumnName = "id", updatable = false)
    })
    private View view;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", updatable = false)
    })
    private TableColumn column;

    @Override
    public int compareTo(ViewColumn tableColumn) {
        return Integer.compare(this.ordinalPosition, tableColumn.getOrdinalPosition());
    }
}
