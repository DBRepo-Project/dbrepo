package at.tuwien.entities.database;

import at.tuwien.entities.container.Container;
import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.elasticsearch.annotations.Document;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.elasticsearch.annotations.FieldType;
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
@Document(indexName = "databaseindex", createIndex = false)
@Where(clause = "deleted is null")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "update mdb_databases set deleted = NOW() where id = ?")
@javax.persistence.Table(name = "mdb_databases", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id", "internalName"})
})
public class Database {

    @Field(type = FieldType.Integer)
    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "database-sequence")
    @GenericGenerator(
            name = "database-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_databases_seq")
    )
    private Long id;

    @Field(index = false)
    @ManyToOne(fetch = FetchType.EAGER, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "Creator", referencedColumnName = "UserID")
    })
    private User creator;

    @Field(index = false)
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "id", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Container container;

    @Field(type = FieldType.Text)
    @Column(nullable = false)
    private String name;

    @Field(index = false)
    @ElementCollection
    @CollectionTable(name = "mdb_databases_subjects", joinColumns = {
            @JoinColumn(name = "dbid", referencedColumnName = "id")
    })
    private List<String> subjects;

    @Field(type = FieldType.Text)
    @Column(nullable = false)
    private String internalName;

    @Field(type = FieldType.Text)
    @Column(nullable = false)
    private String exchange;

    @Field(type = FieldType.Text)
    @Column
    private String description;

    @Field(type = FieldType.Text)
    @Column
    private String publisher;

    @Field(type = FieldType.Integer)
    @Column
    private Short publicationYear;

    @Field(index = false)
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "contactperson", referencedColumnName = "UserID", insertable = false, updatable = false)
    })
    private User contact;

    @Field(index = false)
    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private List<Table> tables;

    @Field(type = FieldType.Boolean)
    @Column(nullable = false)
    private Boolean isPublic;

    @Field(index = false)
    @Column(columnDefinition = "enum('EN', 'DE', 'OTHER')")
    @Enumerated(EnumType.STRING)
    private LanguageType language;

    @Field(index = false)
    @OneToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "License", referencedColumnName = "identifier")
    })
    private License license;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @Column
    private Instant deleted;

}
