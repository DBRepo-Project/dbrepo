package at.tuwien.entities.database;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.CascadeType;
import jakarta.persistence.*;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import jakarta.persistence.OrderBy;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
@jakarta.persistence.Table(name = "mdb_databases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cid", "internalName"})
})
@NamedQueries({
        @NamedQuery(name = "Database.findAllDesc", query = "select d from Database d order by d.created desc"),
        @NamedQuery(name = "Database.findByInternalName", query = "select d from Database d where d.internalName = ?1"),
        @NamedQuery(name = "Database.findAllOnlyIds", query = "select d.id from Database d order by d.created desc"),
        @NamedQuery(name = "Database.findReadAccess", query = "select distinct d from Database d join DatabaseAccess a on a.hdbid = d.id and a.huserid = ?1"),
        @NamedQuery(name = "Database.findWriteAccess", query = "select distinct d from Database d join DatabaseAccess a on a.hdbid = d.id and a.huserid = ?1 where a.type = 'WRITE_OWN' or a.type = 'WRITE_ALL'"),
        @NamedQuery(name = "Database.findConfigureAccess", query = "select distinct d from Database d where d.ownedBy = ?1"),
        @NamedQuery(name = "Database.findPublicOrMine", query = "select distinct d from Database d where d.id = ?1 and (d.isPublic = true or d.ownedBy = ?2)"),
        @NamedQuery(name = "Database.findPublic", query = "select distinct d from Database d where d.isPublic = true and d.id = ?1"),
})
public class Database implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "databases-sequence")
    @GenericGenerator(name = "databases-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "created_by", columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @Column(nullable = false)
    private Long cid;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Container container;

    @Column(nullable = false)
    private String name;

    @Column(nullable = false)
    private String internalName;

    @Column(name = "exchange_name", nullable = false, updatable = false)
    private String exchangeName;

    @Column(columnDefinition = "TEXT")
    private String description;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "contact_person", columnDefinition = "VARCHAR(36)")
    private UUID contactPerson;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "contact_person", referencedColumnName = "ID", updatable = false, insertable = false)
    })
    private User contact;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE}, mappedBy = "database")
    @Where(clause = "identifier_type='DATABASE'")
    @OrderBy("id DESC")
    private List<Identifier> identifiers;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE}, mappedBy = "database")
    @Where(clause = "identifier_type='SUBSET'")
    @OrderBy("id DESC")
    private List<Identifier> subsets;

    @ToString.Exclude
    @OrderBy("id DESC")
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<Table> tables;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<View> views;

    @ToString.Exclude
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<DatabaseAccess> accesses;

    @Column(nullable = false)
    private Boolean isPublic;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
