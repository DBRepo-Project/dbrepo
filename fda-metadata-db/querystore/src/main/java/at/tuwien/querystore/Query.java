package at.tuwien.querystore;

import at.tuwien.api.identifier.IdentifierDto;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.List;

@Data
@Entity
@javax.persistence.Table(name = "qs_queries")
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
@EntityListeners(AuditingEntityListener.class)
public class Query implements Serializable {

    @Id
    @EqualsAndHashCode.Include
    @GeneratedValue(generator = "query-sequence")
    @GenericGenerator(
            name = "query-sequence",
            strategy = "enhanced-sequence",
            parameters = @org.hibernate.annotations.Parameter(name = "sequence_name", value = "qs_queries_seq")
    )
    private Long id;

    @javax.persistence.Column(name = "cid", nullable = false)
    private Long containerId;

    @javax.persistence.Column(name = "dbid", nullable = false)
    private Long databaseId;

    @javax.persistence.Column
    private Instant execution;

    @javax.persistence.Column(nullable = false, columnDefinition = "TEXT")
    private String query;

    @javax.persistence.Column(name = "query_normalized", columnDefinition = "TEXT")
    private String queryNormalized;

    @javax.persistence.Column(name = "query_hash", nullable = false)
    private String queryHash;

    @javax.persistence.Column(name = "result_hash")
    private String resultHash;

    @javax.persistence.Column(name = "result_number")
    private Long resultNumber;

    @Transient
    private IdentifierDto identifier;

    @javax.persistence.Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @javax.persistence.Column(nullable = false)
    private Long createdBy;

    @OneToMany(fetch = FetchType.LAZY, cascade = CascadeType.MERGE)
    private List<at.tuwien.querystore.Table> tables;

    @javax.persistence.Column(name = "last_modified")
    @LastModifiedDate
    private Instant lastModified;

}
