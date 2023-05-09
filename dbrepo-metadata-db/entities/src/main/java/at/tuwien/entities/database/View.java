package at.tuwien.entities.database;

import at.tuwien.entities.database.table.columns.TableColumn;
import at.tuwien.entities.user.User;
import lombok.*;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(ViewKey.class)
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_view")
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
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumn(name = "vdbid", insertable = false, updatable = false)
    private Database database;

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
        final String name = other.toString()
                .replace("`", "");
        if (other.getAlias() != null) {
            final int idx = name.indexOf(' ');
            return this.getInternalName()
                    .equals(name.substring(0, idx));
        }
        return this.getInternalName().equals(name);
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

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
