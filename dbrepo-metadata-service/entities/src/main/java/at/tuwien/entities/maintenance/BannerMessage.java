package at.tuwien.entities.maintenance;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

import static jakarta.persistence.GenerationType.IDENTITY;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_messages")
@NamedQueries({
        @NamedQuery(name = "BannerMessage.findByActive", query = "select m from BannerMessage m where (m.displayStart = null and m.displayEnd = null) or (m.displayStart = null and m.displayEnd >= NOW()) or (m.displayStart <= NOW() and m.displayEnd >= NOW()) or (m.displayStart <= NOW() and m.displayEnd = null)")
})
public class BannerMessage {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, columnDefinition = "ENUM('ERROR','WARNING','INFO')")
    @Enumerated(EnumType.STRING)
    private BannerMessageType type;

    @Column(nullable = false)
    private String message;

    @Column
    private String link;

    @Column
    private String linkText;

    @Column(columnDefinition = "TIMESTAMP")
    private Instant displayStart;

    @Column(columnDefinition = "TIMESTAMP")
    private Instant displayEnd;

}
