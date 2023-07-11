package at.tuwien.entities.identifier;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.elasticsearch.annotations.Field;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_creators", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"pid", "creator_name"})
})
public class Creator {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "creators-sequence")
    @GenericGenerator(name = "creators-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Field(name = "firstname")
    @Column(name = "given_names")
    private String firstname;

    @Field(name = "lastname")
    @Column(name = "family_name")
    private String lastname;

    @Field(name = "creator_name")
    @Column(name = "creator_name", nullable = false)
    private String creatorName;

    @Field(name = "name_type")
    @Column(columnDefinition = "enum('PERSONAL', 'ORGANIZATIONAL')")
    @Enumerated(EnumType.STRING)
    private NameType nameType;

    @Field(name = "name_identifier")
    @Column
    private String nameIdentifier;

    @Field(name = "name_identifier_scheme")
    @Column(columnDefinition = "enum('ROR', 'GRID', 'ISNI', 'ORCID')")
    @Enumerated(EnumType.STRING)
    private NameIdentifierSchemeType nameIdentifierScheme;

    @Field(name = "name_identifier_scheme_uri")
    @Column
    private String nameIdentifierSchemeUri;

    @Column
    private String affiliation;

    @Field(name = "affiliation_identifier")
    @Column
    private String affiliationIdentifier;

    @Field(name = "affiliation_identifier_scheme")
    @Column(columnDefinition = "enum('ROR', 'GRID', 'ISNI')")
    @Enumerated(EnumType.STRING)
    private AffiliationIdentifierSchemeType affiliationIdentifierScheme;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

    public String getApaName() {
        if (this.getNameType() != null && this.getNameType().equals(NameType.ORGANIZATIONAL)) {
            return this.getCreatorName();
        }
        if (this.getFirstname() == null) {
            if (this.getLastname() == null) {
                return this.getCreatorName();
            }
            return this.getLastname();
        }
        return this.getFirstname().charAt(0) + "., " + this.getLastname();
    }

    public String getBibtexName() {
        if (this.getNameType() != null && this.getNameType().equals(NameType.ORGANIZATIONAL)) {
            return this.getCreatorName();
        }
        if (this.getFirstname() == null) {
            if (this.getLastname() == null) {
                return this.getCreatorName();
            }
            return this.getLastname();
        }
        return this.getLastname() + ", " + this.getFirstname();
    }

    public String getIeeeName() {
        if (this.getNameType() != null && this.getNameType().equals(NameType.ORGANIZATIONAL)) {
            return this.getCreatorName();
        }
        if (this.getFirstname() == null) {
            if (this.getLastname() == null) {
                return this.getCreatorName();
            }
            return this.getLastname();
        }
        return this.getFirstname().charAt(0) + ". " + this.getLastname();
    }

}
