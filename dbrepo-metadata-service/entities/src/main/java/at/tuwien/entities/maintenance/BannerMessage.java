package at.tuwien.entities.maintenance;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_banner_messages")
@NamedQueries({
        @NamedQuery(name = "BannerMessage.findByActive", query = "select m from BannerMessage m where (m.displayStart = null and m.displayEnd = null) or (m.displayStart = null and m.displayEnd >= NOW()) or (m.displayStart <= NOW() and m.displayEnd >= NOW()) or (m.displayStart <= NOW() and m.displayEnd = null)")
})
public class BannerMessage {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "messages-sequence")
    @GenericGenerator(name = "messages-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false, columnDefinition = "enum('ERROR','WARNING','INFO')")
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
