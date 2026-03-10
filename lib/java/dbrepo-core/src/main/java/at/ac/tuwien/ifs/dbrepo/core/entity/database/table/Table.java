package at.ac.tuwien.ifs.dbrepo.core.entity.database.table;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.Database;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns.TableColumn;
import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.Constraints;
import at.ac.tuwien.ifs.dbrepo.core.entity.identifier.Identifier;
import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import lombok.extern.log4j.Log4j2;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.annotations.OnDelete;
import org.hibernate.annotations.OnDeleteAction;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.LinkedList;
import java.util.List;
import java.util.UUID;

@Data
@Entity
@Builder
@Log4j2
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_tables", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"tdbid", "internalName"})
})
public class Table {

    @Id
    @Column(updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @Column(updatable = false, nullable = false)
    private UUID tdbid;

    @Column(columnDefinition = "VARCHAR(255)", nullable = false)
    private String ownedBy;

    @Column(name = "tname", columnDefinition = "VARCHAR(64)", nullable = false)
    private String name;

    @Column(nullable = false, columnDefinition = "VARCHAR(64)")
    private String internalName;

    @Column(name = "queue_name", nullable = false, updatable = false)
    private String queueName;

    @Column(name = "tdescription", columnDefinition = "VARCHAR(2048)")
    private String description;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tdbid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Database database;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", insertable = false, updatable = false),
            @JoinColumn(name = "dbid", referencedColumnName = "tdbid", insertable = false, updatable = false)
    })
    @Where(clause = "identifier_type='TABLE'")
    @OrderBy("created DESC")
    private List<Identifier> identifiers = new LinkedList<>();

    @Embedded
    private Constraints constraints;

    @Column(name = "versioned", nullable = false, columnDefinition = "boolean default true")
    private Boolean isVersioned;

    @Column(name = "is_public", nullable = false, columnDefinition = "boolean default true")
    private Boolean isPublic;

    @Column(name = "is_schema_public", nullable = false, columnDefinition = "boolean default true")
    private Boolean isSchemaPublic;

    @Column(name = "num_rows")
    private Long numRows;

    @Column(name = "data_length")
    private Long dataLength;

    @Column(name = "max_data_length")
    private Long maxDataLength;

    @Column(name = "avg_row_length")
    private Long avgRowLength;

    @OrderBy("ordinalPosition")
    @OnDelete(action = OnDeleteAction.CASCADE)
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.ALL, CascadeType.PERSIST}, mappedBy = "table", orphanRemoval = true)
    private List<TableColumn> columns = new LinkedList<>();

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
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

