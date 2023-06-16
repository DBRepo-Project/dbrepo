package at.tuwien.entities.database;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.JoinColumnOrFormula;
import org.hibernate.annotations.JoinColumnsOrFormulas;
import org.hibernate.annotations.JoinFormula;
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
        @UniqueConstraint(columnNames = {"id", "internalName"})
})
@Document(indexName = "database")
public class Database implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private Long id;

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

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "owned_by", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID ownedBy;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User owner;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
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

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "contact_person", referencedColumnName = "ID", updatable = false, insertable = false)
    })
    private User contact;

    @org.springframework.data.annotation.Transient
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
