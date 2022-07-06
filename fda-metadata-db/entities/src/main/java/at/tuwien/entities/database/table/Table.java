package at.tuwien.entities.database.table;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@Document(indexName = "tblindex", createIndex = false)
@IdClass(TableKey.class)
@ToString
@Log4j2
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@javax.persistence.Table(name = "mdb_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tdbid", "internalName"})
})
public class Table {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "table-sequence")
    @GenericGenerator(
            name = "table-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_tables_seq")
    )
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long tdbid;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "UserID")
    })
    private User creator;

    @Column(nullable = false, name = "tname")
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(nullable = false, updatable = false)
    private String topic;

    @Column(name = "tdescription")
    private String description;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "tdbid", insertable = false, updatable = false)
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE, mappedBy = "table")
    @OrderBy("ordinalPosition")
    private List<TableColumn> columns;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @PreRemove
    public void preRemove() {
        this.database = null;
    }

    public boolean equals(FromItem other) {
        final String name = other.toString()
                .replace("`","");
        if (other.getAlias() != null) {
            final int idx = name.indexOf(' ');
            return this.getInternalName()
                    .equals(name.substring(0, idx));
        }
        return this.getInternalName().equals(name);
    }

}

