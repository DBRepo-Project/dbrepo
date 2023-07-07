package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"qid", "dbid"})
})
@NamedQueries({
        @NamedQuery(name = "Identifier.findByDatabaseId", query = "select i from Identifier i where i.database.id = ?1"),
        @NamedQuery(name = "Identifier.findByQueryId", query = "select i from Identifier i where i.queryId = ?1"),
        @NamedQuery(name = "Identifier.findByDatabaseIdAndQueryId", query = "select i from Identifier i where i.database.id = ?1 and i.queryId = ?2"),
})
public class Identifier implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifiers-sequence")
    @GenericGenerator(name = "identifiers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "dbid")
    private Long databaseId;

    @Column(name = "qid")
    private Long queryId;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    private List<Creator> creators;

    @NotBlank
    @Column(nullable = false)
    private String publisher;

    @Column(columnDefinition = "ENUM('ab','aa','af','ak','sq','am','ar','an','hy','as','av','ae','ay','az','bm','ba','eu','be','bn','bh','bi','bs','br','bg','my','ca','km','ch','ce','ny','zh','cu','cv','kw','co','cr','hr','cs','da','dv','nl','dz','en','eo','et','ee','fo','fj','fi','fr','ff','gd','gl','lg','ka','de','ki','el','kl','gn','gu','ht','ha','he','hz','hi','ho','hu','is','io','ig','id','ia','ie','iu','ik','ga','it','ja','jv','kn','kr','ks','kk','rw','kv','kg','ko','kj','ku','ky','lo','la','lv','lb','li','ln','lt','lu','mk','mg','ms','ml','mt','gv','mi','mr','mh','ro','mn','na','nv','nd','ng','ne','se','no','nb','nn','ii','oc','oj','or','om','os','pi','pa','ps','fa','pl','pt','qu','rm','rn','ru','sm','sg','sa','sc','sr','sn','sd','si','sk','sl','so','st','nr','es','su','sw','ss','sv','tl','ty','tg','ta','tt','te','th','bo','ti','to','ts','tn','tr','tk','tw','ug','uk','ur','uz','ve','vi','vo','wa','cy','fy','wo','xh','yi','yo','za','zu')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    private List<IdentifierTitle> titles;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    private List<IdentifierDescription> descriptions;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "License", referencedColumnName = "identifier")
    })
    private License license;

    @Column(name = "identifier_type", nullable = false, columnDefinition = "enum('SUBSET', 'DATABASE')")
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

    @Column(updatable = false, columnDefinition = "TIMESTAMP")
    private Instant execution;

    @Column
    private Long resultNumber;

    @Column(nullable = false)
    private Integer publicationYear;

    @Column
    private Integer publicationMonth;

    @Column
    private Integer publicationDay;

    @Column(nullable = false, columnDefinition = "enum('EVERYONE', 'SELF')")
    @Enumerated(EnumType.STRING)
    private VisibilityType visibility;

    @org.springframework.data.annotation.Transient
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "dbid", referencedColumnName = "id", insertable = false, updatable = false),
    })
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL, mappedBy = "identifier")
    private List<RelatedIdentifier> related;

    @Column
    private String doi;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
    })
    private User creator;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @PreRemove
    private void preRemove() {
        this.creator = null;
    }

}


