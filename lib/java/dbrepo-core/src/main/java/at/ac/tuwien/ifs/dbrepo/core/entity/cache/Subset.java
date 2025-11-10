package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.time.Instant;
import java.util.UUID;


@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@RedisHash("subset")
public class Subset {

    @Id
    private UUID id;

    @Column(nullable = false)
    private UUID databaseId;

    @NotNull
    private String query;

    @NotNull
    private String queryNormalized;

    @NotNull
    private String queryHash;

    @NotNull
    private String resultHash;

    @NotNull
    private Long resultNumber;

    @NotNull
    private Boolean isPersisted;

    private SubsetType type;

    private String ownedBy;

    @NotNull
    private Instant execution;

    @TimeToLive
    private Long exp;
}
