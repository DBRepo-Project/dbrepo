package at.tuwien.entities.identifier;

import at.tuwien.converters.IdentifierFunderTypeConverter;
import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.io.Serializable;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifier_funders")
public class IdentifierFunder implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "identifier-titles-sequence")
    @GenericGenerator(name = "identifier-titles-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Field(name = "funder_name")
    @Column(nullable = false)
    private String funderName;

    @Field(name = "funder_identifier")
    @Column(columnDefinition = "TEXT")
    private String funderIdentifier;

    @Field(name = "funder_identifier_type")
    @Column(name="funder_identifier_type", columnDefinition = "enum('crossref_funder_id', 'ror', 'gnd', 'isni', 'other')")
    @Convert(converter = IdentifierFunderTypeConverter.class)
    private IdentifierFunderType funderIdentifierType;

    @Field(name = "scheme_uri")
    @Column(columnDefinition = "TEXT")
    private String schemeUri;

    @Field(name = "award_number")
    @Column
    private String awardNumber;

    @Field(name = "award_title")
    @Column(columnDefinition = "TEXT")
    private String awardTitle;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


