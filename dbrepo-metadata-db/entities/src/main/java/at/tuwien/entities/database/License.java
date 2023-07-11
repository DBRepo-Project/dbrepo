package at.tuwien.entities.database;

import at.tuwien.entities.identifier.Identifier;
import lombok.*;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;

import java.util.List;

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

    @ToString.Exclude
    @org.springframework.data.annotation.Transient
    @OneToMany(fetch = FetchType.LAZY, mappedBy = "license")
    private List<Identifier> identifiers;

}