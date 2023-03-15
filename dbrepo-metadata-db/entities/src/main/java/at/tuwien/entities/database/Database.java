package at.tuwien.entities.database;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.identifier.Identifier;
import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.*;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.persistence.CascadeType;
import javax.persistence.Entity;
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
@javax.persistence.Table(name = "mdb_databases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id", "internalName"})
})
public class Database implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @Column(updatable = false, nullable = false)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "UserID")
    })
    private User creator;

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "owned_by", referencedColumnName = "UserID")
    })
    private User owner;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
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

    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "contact_person", referencedColumnName = "UserID")
    })
    private User contact;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumnsOrFormulas({
            @JoinColumnOrFormula(formula = @JoinFormula(value = "'DATABASE'", referencedColumnName = "identifier_type")),
            @JoinColumnOrFormula(column = @JoinColumn(name = "id", referencedColumnName = "dbid", insertable = false, updatable = false)),
    })
    private Identifier identifier;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<Table> tables;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "vdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<View> views;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
