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
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifier_creators")
public class Creator {

    @Id
    @org.springframework.data.annotation.Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "creators-sequence")
    @GenericGenerator(name = "creators-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(name = "given_names")
    private String firstname;

    @Column(name = "family_name")
    private String lastname;

    @Column(name = "creator_name", nullable = false)
    private String creatorName;

    @Column(columnDefinition = "enum('PERSONAL', 'ORGANIZATIONAL')")
    @Enumerated(EnumType.STRING)
    private NameType nameType;

    @Column
    private String nameIdentifier;

    @Column(columnDefinition = "enum('ROR', 'GRID', 'ISNI', 'ORCID')")
    @Enumerated(EnumType.STRING)
    private NameIdentifierSchemeType nameIdentifierScheme;

    @Column
    private String nameIdentifierSchemeUri;

    @Column
    private String affiliation;

    @Column
    private String affiliationIdentifier;

    @Column(columnDefinition = "enum('ROR', 'GRID', 'ISNI')")
    @Enumerated(EnumType.STRING)
    private AffiliationIdentifierSchemeType affiliationIdentifierScheme;

    private String affiliationIdentifierSchemeUri;

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
