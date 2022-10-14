package at.tuwien.querystore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import javax.persistence.Column;
import javax.persistence.Table;
import java.io.Serializable;
import java.math.BigInteger;
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

    @javax.persistence.Column(nullable = false)
    private Long cid;

    @javax.persistence.Column(nullable = false)
    private Long dbid;

    @javax.persistence.Column
    @Schema(example = "2022-01-01 08:00:00.000")
    private Instant execution;

    @javax.persistence.Column(nullable = false, columnDefinition = "TEXT")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @javax.persistence.Column(name = "query_normalized", columnDefinition = "TEXT")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @javax.persistence.Column(name = "query_hash", nullable = false)
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @javax.persistence.Column(name = "result_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @javax.persistence.Column(name = "result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @javax.persistence.Column(nullable = false)
    private Boolean isPersisted;

    @javax.persistence.Column(columnDefinition = "enum('QUERY', 'VIEW')")
    @Enumerated(EnumType.STRING)
    private QueryType type;

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
