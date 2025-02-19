package at.tuwien.entities.semantics;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
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
@Table(name = "mdb_ontologies")
@NamedQueries({
        @NamedQuery(name = "Ontology.findAll", query = "select o from Ontology o order by sparqlEndpoint desc"),
        @NamedQuery(name = "Ontology.findAllProcessable", query = "select o from Ontology o where o.sparqlEndpoint != null or o.rdfPath != null order by sparqlEndpoint desc"),
        @NamedQuery(name = "Ontology.findByUriPattern", query = "select o from Ontology o where o.uriPattern like ?1"),
})
public class Ontology {

    @Id
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(nullable = false, updatable = false, columnDefinition = "VARCHAR(36)")
    private UUID id;

    @Column(nullable = false, unique = true)
    private String uri;

    @Column
    private String uriPattern;

    @Column(nullable = false, unique = true, columnDefinition = "VARCHAR(8)")
    private String prefix;

    @Column
    private String sparqlEndpoint;

    @Column
    private String rdfPath;

    @EqualsAndHashCode.Exclude
    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @EqualsAndHashCode.Exclude
    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
