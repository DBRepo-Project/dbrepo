package at.tuwien.entities.database.table;

import at.tuwien.entities.database.DatabaseAccess;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.database.Database;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@Log4j2
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_tables")
public class Table {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "tables-sequence")
    @GenericGenerator(name = "tables-sequence", strategy = "increment")
    @Column(name = "ID", updatable = false, nullable = false)
    private Long id;

    @Column(updatable = false, nullable = false)
    private Long tdbid;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "created_by", columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;


    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @Column(name = "tname", nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(name = "queue_name", nullable = false, updatable = false)
    private String queueName;

    @Column(name = "routing_key", nullable = false, updatable = false)
    private String routingKey;

    @Column(name = "tdescription", columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    @OrderBy("ordinalPosition")
    private List<TableColumn> columns;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "dbid", referencedColumnName = "tdbid", insertable = false, updatable = false)
    })
    @Where(clause = "identifier_type='TABLE'")
    @OrderBy("id DESC")
    private List<Identifier> identifiers;

    @Embedded
    private Constraints constraints;

    @Column(name = "versioned", columnDefinition = "boolean default true")
    private Boolean isVersioned;

    @Column(name = "num_rows")
    private Long numRows;

    @Column(name = "data_length")
    private Long dataLength;

    @Column(name = "max_data_length")
    private Long maxDataLength;

    @Column(name = "avg_row_length")
    private Long avgRowLength;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @Column(name = "processed_constraints", nullable = false)
    private Boolean processedConstraints;

    @Override
    public boolean equals(Object o) {
        if (o == this) {
            return true;
        }
        if (!(o instanceof Table other)) {
            return false;
        }
        return this.id.equals(other.getId());
    }

    /**
     * KEEP THIS FUNCTION HERE! IT WILL BREAK CODE!
     * Custom equality function implementation.
     *
     * @param other The other table
     * @return True if tables are equal, false otherwise
     */
    public boolean equals(FromItem other) {
        if (other == null) {
            return false;
        }
        final net.sf.jsqlparser.schema.Table table = (net.sf.jsqlparser.schema.Table) other;
        return this.internalName.equals(table.getName().replace("`", ""));
    }

}

