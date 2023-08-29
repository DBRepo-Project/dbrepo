package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifiers")
@NamedQueries({
        @NamedQuery(name = "Identifier.findAllDatabaseIdentifiers", query = "select i from Identifier i where i.type = 'DATABASE'"),
        @NamedQuery(name = "Identifier.findAllSubsetIdentifiers", query = "select i from Identifier i where i.type = 'SUBSET'"),
        @NamedQuery(name = "Identifier.findDatabaseIdentifier", query = "select i from Identifier i where i.databaseId = ?1 and i.type = 'DATABASE'"),
        @NamedQuery(name = "Identifier.findSubsetIdentifier", query = "select i from Identifier i where i.databaseId = ?1 and i.queryId = ?2 and i.type = 'SUBSET'"),
        @NamedQuery(name = "Identifier.findViewIdentifier", query = "select i from Identifier i where i.databaseId = ?1 and i.viewId = ?2 and i.type = 'VIEW'"),
})
public class Identifier implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifiers-sequence")
    @GenericGenerator(name = "identifiers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "dbid", nullable = false)
    private Long databaseId;

    @Column(name = "qid")
    private Long queryId;

    @Column(name = "vid")
    private Long viewId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    @OrderBy("id")
    private List<Creator> creators;

    @NotBlank
    @Column(nullable = false)
    private String publisher;

    @Column(columnDefinition = "ENUM('ab','aa','af','ak','sq','am','ar','an','hy','as','av','ae','ay','az','bm','ba','eu','be','bn','bh','bi','bs','br','bg','my','ca','km','ch','ce','ny','zh','cu','cv','kw','co','cr','hr','cs','da','dv','nl','dz','en','eo','et','ee','fo','fj','fi','fr','ff','gd','gl','lg','ka','de','ki','el','kl','gn','gu','ht','ha','he','hz','hi','ho','hu','is','io','ig','id','ia','ie','iu','ik','ga','it','ja','jv','kn','kr','ks','kk','rw','kv','kg','ko','kj','ku','ky','lo','la','lv','lb','li','ln','lt','lu','mk','mg','ms','ml','mt','gv','mi','mr','mh','ro','mn','na','nv','nd','ng','ne','se','no','nb','nn','ii','oc','oj','or','om','os','pi','pa','ps','fa','pl','pt','qu','rm','rn','ru','sm','sg','sa','sc','sr','sn','sd','si','sk','sl','so','st','nr','es','su','sw','ss','sv','tl','ty','tg','ta','tt','te','th','bo','ti','to','ts','tn','tr','tk','tw','ug','uk','ur','uz','ve','vi','vo','wa','cy','fy','wo','xh','yi','yo','za','zu')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierTitle> titles;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierDescription> descriptions;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    @OrderBy("id")
    private List<IdentifierFunder> funders;

    @ManyToMany(fetch = FetchType.LAZY)
    @JoinTable(
            name = "mdb_identifier_licenses",
            joinColumns = @JoinColumn(name = "pid", referencedColumnName = "id"),
            inverseJoinColumns = @JoinColumn(name = "license_id", referencedColumnName = "identifier")
    )
    private List<License> licenses;

    @Column(name = "identifier_type", nullable = false, columnDefinition = "enum('SUBSET', 'DATABASE', 'VIEW')")
    @Enumerated(EnumType.STRING)
    private IdentifierType type;

    @Column(columnDefinition = "TEXT")
    private String query;

    @Field(name = "query_normalized")
    @Column(columnDefinition = "TEXT")
    private String queryNormalized;

    @Field(name = "query_hash")
    @Column
    private String queryHash;

    @Field(name = "result_hash")
    @Column
    private String resultHash;

    @Column(updatable = false, columnDefinition = "TIMESTAMP")
    private Instant execution;

    @Field(name = "result_number")
    @Column
    private Long resultNumber;

    @Field(name = "publication_year")
    @Column(nullable = false)
    private Integer publicationYear;

    @Field(name = "publication_month")
    @Column
    private Integer publicationMonth;

    @Field(name = "publication_day")
    @Column
    private Integer publicationDay;

    @Column(nullable = false, columnDefinition = "enum('EVERYONE', 'SELF')")
    @Enumerated(EnumType.STRING)
    private VisibilityType visibility;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToOne(fetch = FetchType.LAZY, cascade = {CascadeType.PERSIST, CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "dbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @Field(name = "related_identifiers")
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    @OrderBy("id")
    private List<RelatedIdentifier> relatedIdentifiers;

    @Column
    private String doi;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Field(name = "last_modified")
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}


