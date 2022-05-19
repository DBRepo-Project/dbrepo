package at.tuwien.entities.identifier;

import at.tuwien.entities.user.User;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
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
    @ToString.Include
    @GeneratedValue(generator = "creator-sequence")
    @GenericGenerator(
            name = "creator-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "mdb_creators_seq")
    )
    private Long id;

    @Id
    @EqualsAndHashCode.Include
    private Long pid;

    @Column(nullable = false)
    private String firstname;

    @Column(nullable = false)
    private String lastname;

    @Column
    private String affiliation;

    @Column
    private String orcid;

    @ToString.Exclude
    @ManyToOne(cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    @JoinColumn(name = "pid", referencedColumnName = "id", insertable = false, updatable = false)
    private Identifier identifier;

    @Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "UserID")
    })
    private User creator;

    @Column
    @LastModifiedDate
    private Instant lastModified;

}
