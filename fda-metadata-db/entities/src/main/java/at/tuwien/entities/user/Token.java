package at.tuwien.entities.user;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.SQLDelete;
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
@EntityListeners(AuditingEntityListener.class)
@EqualsAndHashCode(onlyExplicitlyIncluded = true)
@Table(name = "mdb_tokens")
public class Token {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(strategy = GenerationType.AUTO)
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false, updatable = false)
    private Long creator;

    @Transient
    @ToString.Exclude
    @Schema(example = "5891b5b522d5df086d0ff0b110fbd9d21bb4fc7163af34d08286a2e846f6be03")
    private String token;

    @Column(nullable = false, updatable = false)
    private String tokenHash;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private Instant created;

    @Column(nullable = false, updatable = false)
    private Instant expires;

    @Column(nullable = false, updatable = false)
    private Instant lastUsed;

}
