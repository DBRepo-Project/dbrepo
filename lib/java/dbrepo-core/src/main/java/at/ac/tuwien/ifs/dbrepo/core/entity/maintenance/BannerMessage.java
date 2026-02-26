package at.ac.tuwien.ifs.dbrepo.core.entity.maintenance;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

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
        @NamedQuery(name = "BannerMessage.findByActive", query = "select m from BannerMessage m where (m.displayStart is null and m.displayEnd is null) or (m.displayStart is null and m.displayEnd >= current_timestamp()) or (m.displayStart <= current_timestamp() and m.displayEnd >= current_timestamp()) or (m.displayStart <= current_timestamp() and m.displayEnd is null)")
})
public class BannerMessage {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(columnDefinition = "VARCHAR(36)")
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

    @PrePersist
    public void prePersist() {
        if (this.id == null) {
            this.id = UUID.randomUUID();
        }
    }

}
