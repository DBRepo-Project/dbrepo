package at.ac.tuwien.ifs.dbrepo.querystore;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;
import org.hibernate.annotations.GenericGenerator;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.Instant;
import java.util.UUID;

@Data
@Entity
@jakarta.persistence.Table(name = "qs_queries")
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

    @jakarta.persistence.Column(nullable = false, columnDefinition = "TEXT")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String query;

    @jakarta.persistence.Column(name = "query_normalized", columnDefinition = "TEXT")
    @Schema(example = "SELECT `id` FROM `air_quality`")
    private String queryNormalized;

    @jakarta.persistence.Column(name = "query_hash", nullable = false)
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String queryHash;

    @jakarta.persistence.Column(name = "result_hash")
    @Schema(example = "17e682f060b5f8e47ea04c5c4855908b0a5ad612022260fe50e11ecb0cc0ab76")
    private String resultHash;

    @jakarta.persistence.Column(name = "result_number")
    @Schema(example = "1")
    private Long resultNumber;

    @jakarta.persistence.Column(nullable = false)
    private Boolean isPersisted;

    @jakarta.persistence.Column(nullable = false, updatable = false)
    @CreatedDate
    private Instant created;

    @jakarta.persistence.Column(nullable = false, updatable = false)
    private Instant executed;

    @jakarta.persistence.Column(nullable = false)
    private UUID createdBy;

}
