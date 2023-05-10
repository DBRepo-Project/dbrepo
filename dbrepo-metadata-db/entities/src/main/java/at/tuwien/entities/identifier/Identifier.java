package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.database.LanguageType;
import at.tuwien.entities.database.License;
import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import jakarta.validation.constraints.NotBlank;
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
        @UniqueConstraint(columnNames = {"qid", "cid", "dbid"})
})
public class Identifier implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifiers-sequence")
    @GenericGenerator(name = "identifiers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "cid", nullable = false)
    private Long containerId;

    @Column(name = "dbid", nullable = false)
    private Long databaseId;

    @Column(name = "qid")
    private Long queryId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
    })
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    @NotBlank
    @Column(nullable = false)
    private String publisher;

    @Column(columnDefinition = "enum('EN', 'DE', 'OTHER')")
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

    @OneToOne(fetch = FetchType.LAZY, cascade = {})
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
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @PreRemove
    private void preRemove() {
        this.creator = null;
        this.related.forEach(r -> r.setCreator(null));
        this.creators.forEach(c -> c.setCreator(null));
    }

}


