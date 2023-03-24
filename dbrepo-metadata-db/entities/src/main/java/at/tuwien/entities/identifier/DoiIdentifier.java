package at.tuwien.entities.identifier;

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
@javax.persistence.Table(name = "mdb_doi_identifiers")
public class DoiIdentifier {

    @Id
    @Column(nullable = false, updatable = false)
    String doi;

    @OneToOne(optional = false)
    @JoinColumn(nullable = false, unique = true)
    Identifier identifier;
}
