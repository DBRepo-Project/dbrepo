package at.tuwien.entities.database;

import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_view")
@NamedQueries({
        @NamedQuery(name = "View.findAllPublicByDatabaseId", query = "select v from View v where v.database.id = ?1 and v.isPublic = true"),
        @NamedQuery(name = "View.findAllPublicOrMineByDatabaseId", query = "select v from View v where v.database.id = ?1 and (v.isPublic = true or v.ownedBy = ?2)"),
        @NamedQuery(name = "View.findPublicByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and v.isPublic = true"),
        @NamedQuery(name = "View.findPublicOrMineByDatabaseIdAndId", query = "select v from View v where v.database.id = ?1 and v.id = ?2 and (v.isPublic = true or v.ownedBy = ?3)")
})
public class View {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID vdbid;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @Column(name = "vname", nullable = false, columnDefinition = "VARCHAR(64)")
    private String name;

    @Column(nullable = false, columnDefinition = "VARCHAR(64)")
    private String internalName;

    @Column(name = "public", nullable = false, columnDefinition = "boolean default true")
    private Boolean isPublic;

    @Column(name = "is_schema_public", nullable = false, columnDefinition = "boolean default true")
    private Boolean isSchemaPublic;

    @Column(name = "initialview", nullable = false)
    private Boolean isInitialView;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @Column(nullable = false, columnDefinition = "VARCHAR(255)")
    private String queryHash;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "vid", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "dbid", referencedColumnName = "vdbid", insertable = false, updatable = false)
    })
    @Where(clause = "identifier_type='VIEW'")
    @OrderBy("id DESC")
    private List<Identifier> identifiers;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "vdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @ToString.Exclude
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "view")
    @OrderBy("ordinalPosition")
    private List<ViewColumn> columns;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
