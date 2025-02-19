package at.tuwien.entities.database.table.constraints.primaryKey;

import at.tuwien.entities.database.table.Table;
import at.tuwien.entities.database.table.columns.TableColumn;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode
@jakarta.persistence.Table(name = "mdb_constraints_primary_key")
public class PrimaryKey {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "pkid", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY, cascade = {CascadeType.MERGE})
    @JoinColumns({
            @JoinColumn(name = "cid", referencedColumnName = "id", nullable = false)
    })
    private TableColumn column;
}
