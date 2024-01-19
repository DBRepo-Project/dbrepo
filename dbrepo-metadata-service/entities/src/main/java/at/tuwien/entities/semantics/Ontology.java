package at.tuwien.entities.semantics;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.hibernate.annotations.JdbcTypeCode;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EntityListeners(AuditingEntityListener.class)
@Table(name = "mdb_ontologies")
@NamedQueries({
        @NamedQuery(name = "Ontology.findAll", query = "select o from Ontology o order by sparqlEndpoint desc"),
        @NamedQuery(name = "Ontology.findAllProcessable", query = "select o from Ontology o where o.sparqlEndpoint != null or o.rdfPath != null order by sparqlEndpoint desc"),
})
public class Ontology {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "ontologies-sequence")
    @GenericGenerator(name = "ontologies-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

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

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
