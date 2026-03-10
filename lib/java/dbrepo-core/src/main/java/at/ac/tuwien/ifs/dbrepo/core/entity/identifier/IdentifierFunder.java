package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcType;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.dialect.PostgreSQLEnumJdbcType;
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
    @Column(updatable = false, nullable = false, columnDefinition = "UUID DEFAULT gen_random_uuid()")
    private UUID id;

    @Column
    private Integer ordinalPosition;

    @Column(nullable = false)
    private String funderName;

    @Column(columnDefinition = "TEXT")
    private String funderIdentifier;

    @Enumerated(EnumType.STRING)
    @JdbcType(PostgreSQLEnumJdbcType.class)
    @Column(name = "funder_identifier_type", columnDefinition = "ENUM('CROSSREF_FUNDER_ID', 'ROR', 'GND', 'ISNI', 'OTHER')")
    private IdentifierFunderType funderIdentifierType;

    @Column(columnDefinition = "TEXT")
    private String schemeUri;

    @Column
    private String awardNumber;

    @Column(columnDefinition = "TEXT")
    private String awardTitle;

    @ToString.Exclude
    @EqualsAndHashCode.Exclude
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


