package at.ac.tuwien.ifs.dbrepo.core.entity.database;

import at.ac.tuwien.ifs.dbrepo.core.entity.container.Container;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import at.ac.tuwien.ifs.dbrepo.core.entity.user.User;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder(toBuilder = true)
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_databases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"cid", "internalName"})
})
@NamedQueries({
        @NamedQuery(name = "Database.findAllDesc", query = "select distinct d from Database d order by d.created desc"),
        @NamedQuery(name = "Database.findAllByInternalNameDesc", query = "select distinct d from Database d where d.internalName = ?1 order by d.created desc"),
        @NamedQuery(name = "Database.findAllAtLestReadAccessDesc", query = "select distinct d from Database d where exists(select a.hdbid from DatabaseAccess a join User u on u.id = a.huserid where u.username = ?1 and a.hdbid = d.id) order by d.created desc"),
        @NamedQuery(name = "Database.findAllPublicOrSchemaPublicDesc", query = "select distinct d from Database d where d.isPublic = true or d.isSchemaPublic = true order by d.created desc"),
        @NamedQuery(name = "Database.findAllPublicOrSchemaPublicOrReadAccessDesc", query = "select distinct d from Database d where d.isPublic = true or d.isSchemaPublic = true or exists(select a.hdbid from DatabaseAccess a join User u on u.id = a.huserid and u.username = ?1 and a.hdbid = d.id) order by d.created desc"),
        @NamedQuery(name = "Database.findAllPublicOrSchemaPublicOrReadAccessByInternalNameDesc", query = "select distinct d from Database d where (d.isPublic = true or d.isSchemaPublic = true) and d.internalName = ?2 or exists(select a.hdbid from DatabaseAccess a join User u on u.id = a.huserid and u.username = ?1 and a.hdbid = d.id) order by d.created desc"),
        @NamedQuery(name = "Database.findAllPublicOrSchemaPublicByInternalNameDesc", query = "select distinct d from Database d where (d.isPublic = true or d.isSchemaPublic = true) and d.internalName = ?1 order by d.created desc"),
})
public class Database implements Serializable {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "grafana_dashboard_uid")
    private String dashboardUid;

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @Column(nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID cid;

    @ToString.Exclude
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

    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "contact_person", columnDefinition = "VARCHAR(36)")
    private UUID contactPerson;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "contact_person", referencedColumnName = "ID", updatable = false, insertable = false)
    })
    private User contact;

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE}, mappedBy = "database")
    @Where(clause = "identifier_type='DATABASE'")
    @OrderBy("created DESC")
    private List<Identifier> identifiers = new LinkedList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE}, mappedBy = "database")
    @Where(clause = "identifier_type='SUBSET'")
    @OrderBy("created DESC")
    private List<Identifier> subsets = new LinkedList<>();

    @OrderBy("created DESC")
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table> tables = new LinkedList<>();

    @OrderBy("created DESC")
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<View> views = new LinkedList<>();

    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "database", orphanRemoval = true)
    private List<DatabaseAccess> accesses = new LinkedList<>();

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isPublic;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isSchemaPublic;

    @Column(nullable = false, columnDefinition = "boolean default true")
    private Boolean isDashboardEnabled;

    @Lob
    @Basic(fetch = FetchType.LAZY)
    @Column(columnDefinition = "LONGBLOB")
    private byte[] image;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP default NOW()")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
