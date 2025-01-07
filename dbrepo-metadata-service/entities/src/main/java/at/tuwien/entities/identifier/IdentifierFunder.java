package at.tuwien.entities.identifier;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

import static jakarta.persistence.GenerationType.IDENTITY;

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
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

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

}


