package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifiers")
@NamedQueries({
        @NamedQuery(name = "Identifier.findAllDatabaseIdentifiers", query = "select i from Identifier i where i.type = 'DATABASE' ORDER BY i.id DESC"),
        @NamedQuery(name = "Identifier.findAllSubsetIdentifiers", query = "select i from Identifier i where i.type = 'SUBSET' ORDER BY i.id DESC"),
        @NamedQuery(name = "Identifier.findDatabaseIdentifier", query = "select i from Identifier i where i.database.id = ?1 and i.type = 'DATABASE' ORDER BY i.id DESC"),
        @NamedQuery(name = "Identifier.findSubsetIdentifier", query = "select i from Identifier i where i.database.id = ?1 and i.queryId = ?2 and i.type = 'SUBSET' ORDER BY i.id DESC"),
        @NamedQuery(name = "Identifier.findViewIdentifier", query = "select i from Identifier i where i.database.id = ?1 and i.viewId = ?2 and i.type = 'VIEW' ORDER BY i.id DESC"),
        @NamedQuery(name = "Identifier.findEarliest", query = "select i from Identifier i ORDER BY i.created ASC limit 1"),
})
public class Identifier implements Serializable {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "qid", columnDefinition = "VARCHAR(36)")
    private UUID queryId;

    @Column(name = "tid", columnDefinition = "VARCHAR(36)")
    private UUID tableId;

    @Column(name = "vid", columnDefinition = "VARCHAR(36)")
    private UUID viewId;

    /**
     * Creators are created/updated/deleted by the Identifier entity.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "identifier")
    @OrderBy("id")
    private List<Creator> creators;

    @Column(nullable = false)
    private String publisher;

    @Column(nullable = false, columnDefinition = "ENUM('DRAFT', 'PUBLISHED')")
    @Enumerated(EnumType.STRING)
    private IdentifierStatusType status;

    @Column(columnDefinition = "ENUM('ab','aa','af','ak','sq','am','ar','an','hy','as','av','ae','ay','az','bm','ba','eu','be','bn','bh','bi','bs','br','bg','my','ca','km','ch','ce','ny','zh','cu','cv','kw','co','cr','hr','cs','da','dv','nl','dz','en','eo','et','ee','fo','fj','fi','fr','ff','gd','gl','lg','ka','de','ki','el','kl','gn','gu','ht','ha','he','hz','hi','ho','hu','is','io','ig','id','ia','ie','iu','ik','ga','it','ja','jv','kn','kr','ks','kk','rw','kv','kg','ko','kj','ku','ky','lo','la','lv','lb','li','ln','lt','lu','mk','mg','ms','ml','mt','gv','mi','mr','mh','ro','mn','na','nv','nd','ng','ne','se','no','nb','nn','ii','oc','oj','or','om','os','pi','pa','ps','fa','pl','pt','qu','rm','rn','ru','sm','sg','sa','sc','sr','sn','sd','si','sk','sl','so','st','nr','es','su','sw','ss','sv','tl','ty','tg','ta','tt','te','th','bo','ti','to','ts','tn','tr','tk','tw','ug','uk','ur','uz','ve','vi','vo','wa','cy','fy','wo','xh','yi','yo','za','zu')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    /**
     * Titles are created/updated/deleted by the Identifier entity.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierTitle> titles;

    /**
     * Descriptions are created/updated/deleted by the Identifier entity.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierDescription> descriptions;

    /**
     * Funders are created/updated/deleted by the Identifier entity.
     */
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierFunder> funders;

    /**
     * Licenses are never created/updated/deleted by the Identifier entity.
     */
    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mdb_identifier_licenses",
            joinColumns = @JoinColumn(name = "pid", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "license_id", referencedColumnName = "identifier")
    )
    private List<License> licenses;

    @Column(name = "identifier_type", nullable = false, columnDefinition = "ENUM('SUBSET', 'DATABASE', 'VIEW', 'TABLE')")
    @Enumerated(EnumType.STRING)
    private IdentifierType type;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Column(columnDefinition = "TEXT")
    private String queryNormalized;

    @Column
    private String queryHash;

    @Column
    private String resultHash;

    @Column(updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant execution;

    @Column
    private Long resultNumber;

    @Column(nullable = false)
    private Integer publicationYear;

    @Column
    private Integer publicationMonth;

    @Column
    private Integer publicationDay;

    /**
     * Databases are never created/updated/deleted by the Identifier entity.
     */
    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "dbid", referencedColumnName = "id", nullable = false, updatable = false)
    })
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "identifier")
    @OrderBy("id")
    private List<RelatedIdentifier> relatedIdentifiers;

    @Column
    private String doi;

    @Column(name = "owned_by", nullable = false)
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    private UUID ownedBy;

    /**
     * Users are never created/updated/deleted by the Identifier entity.
     */
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}


