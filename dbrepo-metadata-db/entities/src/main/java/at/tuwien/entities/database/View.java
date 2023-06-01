package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import lombok.*;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(at.tuwien.entities.database.ViewKey.class)
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

    @Id
    @EqualsAndHashCode.Include
    private Long vcid;

    @Id
    @EqualsAndHashCode.Include
    private Long vdbid;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "vdbid", insertable = false, updatable = false)
    private at.tuwien.entities.database.Database database;

    @Column(name = "vname", nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(name = "public", nullable = false)
    private Boolean isPublic;

    @Column(name = "initialview", nullable = false)
    private Boolean isInitialView;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

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

    @ManyToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinTable(name = "mdb_view_columns",
            joinColumns = {
                    @JoinColumn(name = "vid", referencedColumnName = "id", insertable = false, updatable = false),
                    @JoinColumn(name = "vcid", referencedColumnName = "vcid", insertable = false, updatable = false),
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
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
