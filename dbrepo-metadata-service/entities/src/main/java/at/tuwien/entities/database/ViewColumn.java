package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumnType;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_view_columns", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"view_id", "internal_name"})
})
public class ViewColumn implements Comparable<ViewColumn> {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "view-columns-sequence")
    @GenericGenerator(name = "view-columns-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "view_id", referencedColumnName = "id", nullable = false)
    })
    private View view;

    @Column(nullable = false, columnDefinition = "VARCHAR(64)")
    private String name;

    @Column(name = "internal_name", nullable = false, columnDefinition = "VARCHAR(64)")
    private String internalName;

    @Column(nullable = false, columnDefinition = "ENUM('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR')")
    @Enumerated(EnumType.STRING)
    private TableColumnType columnType;

    @Column(nullable = false, columnDefinition = "BOOLEAN default true")
    private Boolean isNullAllowed;

    @Column(nullable = false)
    private Integer ordinalPosition;

    @Column
    private Long size;

    @Column
    private Long d;

    @Override
    public int compareTo(ViewColumn viewColumn) {
        return Integer.compare(this.ordinalPosition, viewColumn.getOrdinalPosition());
    }
}
