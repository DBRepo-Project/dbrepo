package at.tuwien.entities.database;

import at.tuwien.entities.user.User;
import lombok.*;
import net.sf.jsqlparser.statement.select.FromItem;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(ViewKey.class)
@Document(indexName = "viewindex", createIndex = false)
@EntityListeners(AuditingEntityListener.class)
@javax.persistence.Table(name = "mdb_view")
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

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "UserID")
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

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
