package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import jakarta.persistence.*;
import lombok.*;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
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
@jakarta.persistence.Table(name = "mdb_view")
@NamedQueries({
        @NamedQuery(name = "View.findAllPublicByDatabaseId", query = "select v from View v where v.database.id = ?1 and v.isPublic = true"),
        @NamedQuery(name = "View.findAllPublicOrMineByDatabaseId", query = "select v from View v where v.database.id = ?1 and (v.isPublic = true or v.creator.username = ?2)"),
        @NamedQuery(name = "View.findPublicByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and v.isPublic = true"),
        @NamedQuery(name = "View.findPublicOrMineByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and (v.isPublic = true or v.creator.username = ?3)")
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

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Field(name = "created_by")
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

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

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "vdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

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

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinTable(name = "mdb_view_columns",
            joinColumns = {
                    @JoinColumn(name = "vid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "vdbid", referencedColumnName = "vdbid", insertable = false, updatable = false)
            },
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "ctid", referencedColumnName = "tid", insertable = false, updatable = false),
                    @JoinColumn(name = "cdbid", referencedColumnName = "cdbid", insertable = false, updatable = false)
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
