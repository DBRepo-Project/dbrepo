package at.tuwien.entities.identifier;

import at.tuwien.entities.user.User;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@IdClass(CreatorKey.class)
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_creators")
public class Creator {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "creators-sequence")
    @GenericGenerator(name = "creators-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long pid;

    @Column(name = "given_names")
    private String firstname;

    @Column(name = "family_name")
    private String lastname;

    @Column(nullable = false)
    private String creatorName;

    @Column(columnDefinition = "enum('Personal', 'Organizational')")
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

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", referencedColumnName = "id", insertable = false, updatable = false)
    private Identifier identifier;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", nullable = false, columnDefinition = "VARCHAR(36)", updatable = false)
    })
    private User creator;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
