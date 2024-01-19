package at.tuwien.entities.identifier;

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

    @Column(columnDefinition = "enum('DOI','URL','URN','ARK','arXiv','bibcode','EAN13','EISSN','Handle','IGSN','ISBN','ISTC','LISSN','LSID','PMID','PURL','UPC','w3id')")
    @Enumerated(EnumType.STRING)
    private RelatedType type;

    @Column(columnDefinition = "enum('IsCitedBy','Cites','IsSupplementTo','IsSupplementedBy','IsContinuedBy','Continues','IsDescribedBy','Describes','HasMetadata','IsMetadataFor','HasVersion','IsVersionOf','IsNewVersionOf','IsPreviousVersionOf','IsPartOf','HasPart','IsPublishedIn','IsReferencedBy','References','IsDocumentedBy','Documents','IsCompiledBy','Compiles','IsVariantFormOf','IsOriginalFormOf','IsIdenticalTo','IsReviewedBy','Reviews','IsDerivedFrom','IsSourceOf','IsRequiredBy','Requires','IsObsoletedBy','Obsoletes')")
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


