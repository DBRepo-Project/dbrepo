package at.tuwien.entities.user;

import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.Where;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.time.Instant;

@Data
@Entity
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@Where(clause = "valid_to >= NOW() and processed = false")
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_time_secrets")
public class TimeSecret {

    @Id
    @EqualsAndHashCode.Include
    @GenericGenerator(name = "native", strategy = "native")
    @GeneratedValue(strategy = GenerationType.AUTO, generator = "native")
    private Long id;

    @Column(nullable = false)
    private Long uid;

    @ToString.Exclude
    @Column(nullable = false, updatable = false)
    private String token;

    @Column(nullable = false)
    private Boolean processed;

    @org.springframework.data.annotation.Transient
    @ToString.Exclude
    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.ALL)
    @JoinColumns({
            @JoinColumn(name = "uid", referencedColumnName = "userid", insertable = false, updatable = false)
    })
    private User user;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant created;

    @Column(nullable = false)
    private Instant validTo;

}
