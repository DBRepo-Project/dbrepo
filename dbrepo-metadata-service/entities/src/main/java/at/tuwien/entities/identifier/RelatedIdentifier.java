package at.tuwien.entities.identifier;

import at.tuwien.converters.IdentifierRelatedTypeConverter;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

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
    @GeneratedValue(generator = "related-identifiers-sequence")
    @GenericGenerator(name = "related-identifiers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String value;

    @Column(columnDefinition = "enum('doi', 'url', 'urn', 'ark', 'arxiv', 'bibcode', 'ean13', 'eissn', 'handle', 'igsn', 'isbn', 'istc', 'lissn', 'lsid', 'pmid', 'purl', 'upc', 'w3id')")
    @Convert(converter = IdentifierRelatedTypeConverter.class)
    private RelatedType type;

    @Column(columnDefinition = "enum('is_cited_by', 'cites', 'is_supplement_to', 'is_supplemented_by', 'is_continued_by', 'continues', 'is_described_by', 'describes', 'has_metadata', 'is_metadata_for', 'has_version', 'is_version_of', 'is_new_version_of', 'is_previous_version_of', 'is_part_of', 'has_part', 'is_published_in', 'is_referenced_by', 'references', 'is_documented_by', 'documents', 'is_compiled_by', 'compiles', 'is_variant_form_of', 'is_original_form_of', 'is_identical_to', 'is_reviewed_by', 'reviews', 'is_derived_from', 'is_source_of', 'is_required_by', 'requires', 'is_obsoleted_by', 'obsoletes')")
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


