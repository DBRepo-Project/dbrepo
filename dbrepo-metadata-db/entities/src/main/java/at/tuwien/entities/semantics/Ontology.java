package at.tuwien.entities.semantics;

import at.tuwien.entities.user.User;
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
public class Ontology {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "ontologies-sequence")
    @GenericGenerator(name = "ontologies-sequence", strategy = "increment")
    @Column(updatable = false, nullable = false)
    private Long id;

    @Column(nullable = false)
    private String uri;

    @Column
    private String sparqlEndpoint;

    @ToString.Exclude
    @JdbcTypeCode(java.sql.Types.VARCHAR)
    @Column(name = "createdBy", nullable = false, columnDefinition = "VARCHAR(36)")
    private UUID createdBy;

    @ManyToOne(fetch = FetchType.LAZY, cascade = CascadeType.PERSIST)
    @JoinColumns({
            @JoinColumn(name = "createdBy", referencedColumnName = "ID", insertable = false, updatable = false)
    })
    private User creator;

    @CreatedDate
    @Column(nullable = false, updatable = false, columnDefinition = "TIMESTAMP")
    private Instant created;

    @LastModifiedDate
    @Column(columnDefinition = "TIMESTAMP")
    private Instant lastModified;

}
