package at.tuwien.entities.identifier;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_related_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id", "pid"})
})
public class RelatedIdentifier {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = IDENTITY)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String value;

    @Column(columnDefinition = "ENUM('DOI','URL','URN','ARK','ARXIV','BIBCODE','EAN13','EISSN','HANDLE','IGSN','ISBN','ISTC','LISSN','LSID','PMID','PURL','UPC','W3ID')")
    @Enumerated(EnumType.STRING)
    private RelatedType type;

    @Column(columnDefinition = "ENUM('IS_CITED_BY','CITES','IS_SUPPLEMENT_TO','IS_SUPPLEMENTED_BY','IS_CONTINUED_BY','CONTINUES','IS_DESCRIBED_BY','DESCRIBES','HAS_METADATA','IS_METADATA_FOR','HAS_VERSION','IS_VERSION_OF','IS_NEW_VERSION_OF','IS_PREVIOUS_VERSION_OF','IS_PART_OF','HAS_PART','IS_PUBLISHED_IN','IS_REFERENCED_BY','REFERENCES','IS_DOCUMENTED_BY','DOCUMENTS','IS_COMPILED_BY','COMPILES','IS_VARIANT_FORM_OF','IS_ORIGINAL_FORM_OF','IS_IDENTICAL_TO','IS_REVIEWED_BY','REVIEWS','IS_DERIVED_FROM','IS_SOURCE_OF','IS_REQUIRED_BY','REQUIRES','IS_OBSOLETED_BY','OBSOLETES')")
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


