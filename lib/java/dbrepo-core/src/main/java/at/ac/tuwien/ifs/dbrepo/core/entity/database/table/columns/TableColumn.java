package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_columns", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tID", "internal_name"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumn.findAllByDatabaseId", query = "select c from TableColumn c where c.table.database.id = ?1"),
})
public class TableColumn implements Comparable<TableColumn> {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "Datatype", nullable = false, columnDefinition = "ENUM('CHAR','VARCHAR','BINARY','VARBINARY','TINYBLOB','TINYTEXT','TEXT','BLOB','MEDIUMTEXT','MEDIUMBLOB','LONGTEXT','LONGBLOB','ENUM','SET','REAL','SERIAL','BIT','TINYINT','BOOL','SMALLINT','MEDIUMINT','INT','BIGINT','FLOAT','DOUBLE','DECIMAL','DATE','DATETIME','TIMESTAMP','TIME','YEAR')")
    private TableColumnType columnType;

    @Column
    private Long length;

    @Column(nullable = false, columnDefinition = "BOOLEAN default true")
    private Boolean isNullAllowed;

    @Column(nullable = false)
    private Integer ordinalPosition;

    @Column
    private String conceptUri;

    @Column
    private String unitUri;

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "column")
    private List<ColumnEnum> enums = new LinkedList<>();

    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "column")
    private List<ColumnSet> sets = new LinkedList<>();

    @Column
    private Long size;

    @Column
    private Long d;

    @Column(name = "val_min", columnDefinition = "DECIMAL(65,4)")
    private BigDecimal min;

    @Column(name = "val_max", columnDefinition = "DECIMAL(65,4)")
    private BigDecimal max;

    @Column(columnDefinition = "DECIMAL(65,4)")
    private BigDecimal mean;

    @Column(columnDefinition = "DECIMAL(65,4)")
    private BigDecimal median;

    @Column(name = "std_dev", columnDefinition = "DECIMAL(65,4)")
    private BigDecimal stdDev;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @Override
    public int compareTo(TableColumn tableColumn) {
        return Integer.compare(this.ordinalPosition, tableColumn.getOrdinalPosition());
    }

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
