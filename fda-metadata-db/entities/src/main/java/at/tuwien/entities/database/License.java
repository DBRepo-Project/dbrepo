package at.tuwien.entities.database;

import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@javax.persistence.Table(name = "mdb_licenses", uniqueConstraints = {
        @UniqueConstraint(columnNames = {"uri"})
})
public class License {

    @Id
    @Column(nullable = false, columnDefinition = "enum('MIT', 'GPL-3.0-only', 'BSD-3-Clause', 'BSD-4-Clause', " +
            "'Apache-2.0', 'CC0-1.0', 'CC-BY-4.0')")
    @Enumerated(EnumType.STRING)
    private LicenseIdentifierType identifier;

    @Column(nullable = false)
    private String uri;

}