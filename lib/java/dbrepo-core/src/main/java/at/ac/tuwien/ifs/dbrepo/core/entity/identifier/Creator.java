package at.ac.tuwien.ifs.dbrepo.core.entity.identifier;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_identifier_creators")
public class Creator {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(name = "given_names")
    private String firstname;

    @Column(name = "family_name")
    private String lastname;

    @Column(name = "creator_name", nullable = false)
    private String creatorName;

    @Column(columnDefinition = "ENUM('PERSONAL', 'ORGANIZATIONAL')")
    @Enumerated(EnumType.STRING)
    private NameType nameType;

    @Column
    private String nameIdentifier;

    @Column(columnDefinition = "ENUM('ROR', 'GRID', 'ISNI', 'ORCID')")
    @Enumerated(EnumType.STRING)
    private NameIdentifierSchemeType nameIdentifierScheme;

    @Column
    private String nameIdentifierSchemeUri;

    @Column
    private String affiliation;

    @Column
    private String affiliationIdentifier;

    @Column(columnDefinition = "ENUM('ROR', 'GRID', 'ISNI')")
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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
