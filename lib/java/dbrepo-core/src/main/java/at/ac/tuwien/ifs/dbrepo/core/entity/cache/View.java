package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.persistence.Column;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.List;
import java.util.UUID;


@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@RedisHash("view")
public class View {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String internalName;

    @jakarta.persistence.Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    private Boolean isSchemaPublic;

    @Column(nullable = false)
    private String query;

    @Column(nullable = false)
    private String queryHash;

    @Column(nullable = false)
    private String ownedBy;

    @NotNull
    private List<ViewColumn> columns;

    @TimeToLive
    private Long exp;

}
