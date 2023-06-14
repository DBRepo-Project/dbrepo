package at.tuwien.entities.database.table;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.database.table.constraints.Constraints;
import at.tuwien.entities.user.User;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@Log4j2
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@IdClass(TableKey.class)
@jakarta.persistence.Table(name = "mdb_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tdbid", "internalName"})
})
public class Table {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "tables-sequence")
    @GenericGenerator(name = "tables-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long tdbid;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
    })
    private User creator;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "ownedBy", referencedColumnName = "ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
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
    @ManyToOne(fetch = FetchType.LAZY, cascade = {})
    @JoinColumn(name = "tdbid", insertable = false, updatable = false)
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "table")
    @OrderBy("ordinalPosition")
    private List<TableColumn> columns;

    @Embedded
    private Constraints constraints;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @PreRemove
    public void preRemove() {
        this.creator = null;
        this.columns.forEach(c -> c.setCreator(null));
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

