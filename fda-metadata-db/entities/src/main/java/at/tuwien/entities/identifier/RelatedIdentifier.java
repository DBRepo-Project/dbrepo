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
@Where(clause = "deleted is null")
@EntityListeners(AuditingEntityListener.class)
@SQLDelete(sql = "update mdb_related_identifiers set deleted = NOW() where id = ?")
@Table(name = "mdb_related_identifiers")
public class RelatedIdentifier {

    @Id
    @EqualsAndHashCode.Include
    @ToString.Include
    @GeneratedValue(generator = "related-identifier-sequence")
    @GenericGenerator(
            name = "related-identifier-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_related_identifiers_seq")
    )
    private Long id;

    @Column(nullable = false)
    private Long iid;

    @Column(nullable = false)
    private String value;

    @Column
    @Enumerated(EnumType.STRING)
    private RelatedType type;

    @Column
    @Enumerated(EnumType.STRING)
    private RelationType relation;

    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "iid", referencedColumnName = "id", insertable = false, updatable = false)
    })
    private Identifier identifier;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "created_by", referencedColumnName = "UserID", insertable = false, updatable = false)
    })
    private User creator;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @Column
    @LastModifiedDate
    private Instant lastModified;

    @Column
    private Instant deleted;

}


