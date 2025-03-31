package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.columns;

import com.fasterxml.jackson.annotation.JsonFormat;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
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
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@Table(name = "mdb_units", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
@NamedQueries({
        @NamedQuery(name = "TableColumnUnit.findAll", query = "select u from TableColumnUnit u order by u.name, u.uri asc"),
        @NamedQuery(name = "TableColumnUnit.findByUri", query = "select u from TableColumnUnit u where u.uri = ?1")
})
public class TableColumnUnit {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(updatable = false, nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "VARCHAR(255)")
    private String name;

    @Column(columnDefinition = "TEXT")
    private String description;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    @JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSXXX", timezone = "UTC")
    private Instant created;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinTable(name = "mdb_columns_units",
            inverseJoinColumns = {
                    @JoinColumn(name = "cid", referencedColumnName = "id", insertable = false, updatable = false)
            },
            joinColumns = @JoinColumn(name = "id", referencedColumnName = "id"))
    private List<TableColumn> columns;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
