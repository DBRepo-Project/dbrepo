package at.tuwien.entities.database.table;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.Constraints;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
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

    @Field(name = "database_id")
    @Column(updatable = false, nullable = false)
    private Long tdbid;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "created_by", columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @Column(name = "tname", nullable = false)
    private String name;

    @Field(name = "internal_name")
    @Column(nullable = false)
    private String internalName;

    @Field(name = "queue_name")
    @Column(name = "queue_name", nullable = false, updatable = false)
    private String queueName;

    @Field(name = "routing_key")
    @Column(name = "routing_key", nullable = false, updatable = false)
    private String routingKey;

    @Column(name = "tdescription", columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "table")
    @OrderBy("ordinalPosition")
    private List<TableColumn> columns;

    @Embedded
    private Constraints constraints;

    @Column(name = "versioned", columnDefinition = "boolean default true")
    private Boolean isVersioned;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Field(name = "last_modified")
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

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

