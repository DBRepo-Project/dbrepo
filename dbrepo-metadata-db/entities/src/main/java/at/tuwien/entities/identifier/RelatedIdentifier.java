package at.tuwien.entities.identifier;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@IdClass(RelatedIdentifierKey.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_related_identifiers", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"id", "iid"})
})
public class RelatedIdentifier {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "related-identifiers-sequence")
    @GenericGenerator(name = "related-identifiers-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long iid;

    @Column(nullable = false)
    private String value;

    @Column
    @Enumerated(EnumType.STRING)
    private RelatedType type;

    @Column
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "UserID")
    })
    private User creator;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}


