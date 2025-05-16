package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifier_funders")
public class IdentifierFunder implements Serializable {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column
    private Integer ordinalPosition;

    @Column(nullable = false)
    private String funderName;

    @Column(columnDefinition = "TEXT")
    private String funderIdentifier;

    @Column(name = "funder_identifier_type", columnDefinition = "ENUM('CROSSREF_FUNDER_ID', 'ROR', 'GND', 'ISNI', 'OTHER')")
    @Enumerated(EnumType.STRING)
    private IdentifierFunderType funderIdentifierType;

    @Column(columnDefinition = "TEXT")
    private String schemeUri;

    @Column
    private String awardNumber;

    @Column(columnDefinition = "TEXT")
    private String awardTitle;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}


