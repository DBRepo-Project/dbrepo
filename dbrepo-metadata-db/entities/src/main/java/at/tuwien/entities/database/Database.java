package at.tuwien.entities.database;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import jakarta.persistence.CascadeType;
import jakarta.persistence.*;
import jakarta.persistence.NamedQueries;
import jakarta.persistence.NamedQuery;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
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
@jakarta.persistence.Table(name = "mdb_databases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cid", "internalName"})
})
@Document(indexName = "database")
@NamedQueries({
        @NamedQuery(name = "Database.findReadAccess", query = "select distinct d from Database d join DatabaseAccess a on a.hdbid = d.id and a.huserid = ?1"),
        @NamedQuery(name = "Database.findWriteAccess", query = "select distinct d from Database d join DatabaseAccess a on a.hdbid = d.id and a.huserid = ?1 where a.type = 'WRITE_OWN' or a.type = 'WRITE_ALL'"),
        @NamedQuery(name = "Database.findConfigureAccess", query = "select distinct d from Database d where d.owner.id = ?1"),
        @NamedQuery(name = "Database.findPublicOrMine", query = "select distinct d from Database d where d.id = ?1 and (d.isPublic = true or d.owner.id = ?2)"),
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
    @Column(name = "created_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @Column(nullable = false)
    private Long cid;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(cascade = CascadeType.PERSIST)
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
    @Column(name = "contact_person", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID contactPerson;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "contact_person", referencedColumnName = "ID", updatable = false, insertable = false)
    })
    private User contact;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula = @JoinFormula(value = "'DATABASE'", referencedColumnName = "identifier_type")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "id", referencedColumnName = "dbid", insertable = false, updatable = false)),
    })
    private Identifier identifier;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<Table> tables;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "vdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<View> views;

    @Column(nullable = false)
    private Boolean isPublic;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
