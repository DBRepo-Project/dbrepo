package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
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
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"qid", "dbid"})
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

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(nullable = false)
    private String publisher;

    @Column(columnDefinition = "enum('AB','AA','AF','AK','SQ','AM','AR','AN','HY','AS','AV','AE','AY','AZ','BM','BA','EU','BE','BN','BH','BI','BS','BR','BG','MY','CA','KM','CH','CE','NY','ZH','CU','CV','KW','CO','CR','HR','CS','DA','DV','NL','DZ','EN','EO','ET','EE','FO','FJ','FI','FR','FF','GD','GL','LG','KA','DE','KI','EL','KL','GN','GU','HT','HA','HE','HZ','HI','HO','HU','IS','IO','IG','ID','IA','IE','IU','IK','GA','IT','JA','JV','KN','KR','KS','KK','RW','KV','KG','KO','KJ','KU','KY','LO','LA','LV','LB','LI','LN','LT','LU','MK','MG','MS','ML','MT','GV','MI','MR','MH','RO','MN','NA','NV','ND','NG','NE','SE','NO','NB','NN','II','OC','OJ','OR','OM','OS','PI','PA','PS','FA','PL','PT','QU','RM','RN','RU','SM','SG','SA','SC','SR','SN','SD','SI','SK','SL','SO','ST','NR','ES','SU','SW','SS','SV','TL','TY','TG','TA','TT','TE','TH','BO','TI','TO','TS','TN','TR','TK','TW','UG','UK','UR','UZ','VE','VI','VO','WA','CY','FY','WO','XH','YI','YO','ZA','ZU')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "License", referencedColumnName = "identifier")
    })
    private License license;

    @Column(name = "identifier_type", nullable = false, columnDefinition = "enum('SUBSET', 'DATABASE')")
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

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "dbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "iid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<RelatedIdentifier> related;

    @Column
    private String doi;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "identifier")
    private List<Creator> creators;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Field(name = "last_modified")
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @PreRemove
    private void preRemove() {
        this.creator = null;
        this.related.forEach(r -> r.setCreator(null));
        this.creators.forEach(c -> c.setCreator(null));
    }

}


