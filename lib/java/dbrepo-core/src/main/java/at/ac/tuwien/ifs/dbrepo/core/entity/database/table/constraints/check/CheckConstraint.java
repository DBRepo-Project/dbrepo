package at.ac.tuwien.ifs.dbrepo.core.entity.database.table.constraints.check;

import at.ac.tuwien.ifs.dbrepo.core.entity.database.table.Table;
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
@jakarta.persistence.Table(name = "mdb_constraints_checks")
public class CheckConstraint {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "id", nullable = false, columnDefinition = "VARCHAR(36) DEFAULT UUID()")
    private UUID id;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    @JoinColumns({
            @JoinColumn(name = "tid", referencedColumnName = "id", nullable = false)
    })
    private Table table;

    @Column(name = "checks", nullable = false)
    private String check;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }
}
