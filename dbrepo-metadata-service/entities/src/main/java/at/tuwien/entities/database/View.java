package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.identifier.Identifier;
import jakarta.persistence.*;
import jakarta.persistence.CascadeType;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.Table;
import lombok.*;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_view")
@NamedQueries({
        @NamedQuery(name = "View.findAllPublicByDatabaseId", query = "select v from View v where v.database.id = ?1 and v.isPublic = true"),
        @NamedQuery(name = "View.findAllPublicOrMineByDatabaseId", query = "select v from View v where v.database.id = ?1 and (v.isPublic = true or v.createdBy = ?2)"),
        @NamedQuery(name = "View.findPublicByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and v.isPublic = true"),
        @NamedQuery(name = "View.findPublicOrMineByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and (v.isPublic = true or v.createdBy = ?3)")
})
public class View {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "views-sequence")
    @GenericGenerator(name = "views-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Field(name = "database_id")
    @Column(updatable = false, nullable = false)
    private Long vdbid;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Field(name = "created_by")
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @Column(name = "vname", nullable = false)
    private String name;

    @Field(name = "internal_name")
    @Column(nullable = false)
    private String internalName;

    @Field(name = "is_public")
    @Column(name = "public", nullable = false)
    private Boolean isPublic;

    @Field(name = "is_initial_view")
    @Column(name = "initialview", nullable = false)
    private Boolean isInitialView;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String queryHash;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "vdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(column = @JoinColumn(name = "id", referencedColumnName = "vid", insertable = false, updatable = false)),
            @JoinColumnOrFormula(formula = @JoinFormula(referencedColumnName = "identifier_type", value = "'view'"))
    })
    private Identifier identifier;

    /**
     * KEEP THIS FUNCTION HERE! IT WILL BREAK CODE!
     * Custom equality function implementation.
     *
     * @param other The other view
     * @return True if views are equal, false otherwise
     */
    public boolean equals(FromItem other) {
        if (other == null) {
            return false;
        }
        final net.sf.jsqlparser.schema.Table table = (net.sf.jsqlparser.schema.Table) other;
        return this.internalName.equals(table.getName().replace("`", ""));
    }

    /**
     * Cascade cannot be CascadeType.PERSIST since columns already exist
     */
    @ToString.Exclude
    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "mdb_view_columns",
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id"),
            },
            joinColumns = {
                    @JoinColumn(name = "vid", referencedColumnName = "id"),
            })
    @OrderColumn(name = "position")
    private List<TableColumn> columns;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Field(name = "last_modified")
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
