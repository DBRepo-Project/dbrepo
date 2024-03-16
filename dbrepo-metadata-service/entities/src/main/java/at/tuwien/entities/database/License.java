package at.tuwien.entities.database;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@jakarta.persistence.Table(name = "mdb_licenses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
public class License {

    @Id
    @Column(nullable = false)
    private String identifier;

    @Column(nullable = false, columnDefinition = "TEXT")
    private String uri;

    @Column(columnDefinition = "TEXT")
    private String description;

}