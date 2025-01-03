package at.tuwien.entities.database.table.columns;

import at.tuwien.entities.database.table.Table;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_columns", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tID", "internal_name"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumn.findAllByDatabaseId", query = "select c from TableColumn c where c.table.database.id = ?1"),
})
public class TableColumn implements Comparable<TableColumn> {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy=IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "tID", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @Column(name = "cname", nullable = false, columnDefinition = "VARCHAR(64)")
    private String name;

    @Column(name = "internal_name", nullable = false, columnDefinition = "VARCHAR(64)")
    private String internalName;

    @Column(columnDefinition = "VARCHAR(2048)")
    private String description;

    @Column
    private Long indexLength;

    @Transient
    private String alias;

    @Column(name = "Datatype", nullable = false, columnDefinition = "ENUM('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','SERIAL','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR')")
    @Enumerated(EnumType.STRING)
    private TableColumnType columnType;

    @Column
    private Long length;

    @Column(nullable = false, columnDefinition = "BOOLEAN default true")
    private Boolean isNullAllowed;

    @Column(nullable = false)
    private Integer ordinalPosition;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(name = "mdb_columns_concepts",
            joinColumns = @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
    private TableColumnConcept concept;

    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST})
    @JoinTable(name = "mdb_columns_units",
            joinColumns = @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false),
            inverseJoinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
    private TableColumnUnit unit;

    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "mdb_columns_enums", joinColumns = @JoinColumn(name = "column_id"))
    @Column(name = "value", nullable = false)
    private List<String> enums;

    @ElementCollection(fetch = FetchType.LAZY, targetClass = String.class)
    @CollectionTable(name = "mdb_columns_sets", joinColumns = @JoinColumn(name = "column_id"))
    @Column(name = "value", nullable = false)
    private List<String> sets;

    @Column
    private Long size;

    @Column
    private Long d;

    @Column(name = "val_min")
    private BigDecimal min;

    @Column(name = "val_max")
    private BigDecimal max;

    @Column
    private BigDecimal mean;

    @Column
    private BigDecimal median;

    @Column(name = "std_dev")
    private BigDecimal stdDev;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @Override
    public int compareTo(TableColumn tableColumn) {
        return Integer.compare(this.ordinalPosition, tableColumn.getOrdinalPosition());
    }
}
