package at.tuwien.entities.identifier;

import at.tuwien.entities.database.Database;
import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@Where(clause = "deleted is null")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "update mdb_identifiers set deleted = NOW() where id = ?")
@javax.persistence.Table(name = "mdb_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"qid", "cid", "dbid"})
})
public class Identifier {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(generator = "database-sequence")
    @GenericGenerator(
            name = "database-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_identifiers_seq")
    )
    private Long id;

    @Column(name = "cid", nullable = false)
    private Long containerId;

    @Column(name = "dbid", nullable = false)
    private Long databaseId;

    @Column(name = "qid", nullable = false)
    private Long queryId;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "UserID")
    })
    private User creator;

    @Column(nullable = false)
    private String title;

    @Column(nullable = false)
    private String description;

    @Column(name = "identifier_type", nullable = false, columnDefinition = "enum('SUBSET', 'DATABASE')")
    @Enumerated(EnumType.STRING)
    private IdentifierType type;

    @Column(nullable = false)
    private String query;

    @Column(nullable = false)
    private String queryNormalized;

    @Column(nullable = false)
    private String queryHash;

    @Column(nullable = false)
    private String resultHash;

    @Column(nullable = false)
    private Instant execution;

    @Column(nullable = false)
    private Long resultNumber;

    @Column(nullable = false)
    private Integer publicationYear;

    @Column
    private Integer publicationMonth;

    @Column
    private Integer publicationDay;

    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "dbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @Column(nullable = false, columnDefinition = "enum('EVERYONE', 'TRUSTED', 'SELF')")
    @Enumerated(EnumType.STRING)
    private VisibilityType visibility = VisibilityType.SELF;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "iid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<RelatedIdentifier> related;

    @Column
    private String doi;

    @OneToMany(cascade = CascadeType.ALL, fetch = FetchType.LAZY, mappedBy = "identifier")
    private List<Creator> creators;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @Column
    private Instant deleted;

}


