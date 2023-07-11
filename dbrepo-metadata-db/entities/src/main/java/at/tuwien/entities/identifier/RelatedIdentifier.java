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

    @Column
    @Enumerated(EnumType.STRING)
    private RelatedType type;

    @Column
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumns({
            @JoinColumn(name = "pid", referencedColumnName = "id", updatable = false)
    })
    private Identifier identifier;

}


