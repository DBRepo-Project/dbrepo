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
@RedisHash("database")
public class Database {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String internalName;

    @Column(nullable = false)
    private Boolean isPublic;

    @Column(nullable = false)
    private Boolean isSchemaPublic;

    @Column(nullable = false)
    private Boolean isDashboardEnabled;

    @NotNull
    private Container container;

    @NotNull
    private String ownedBy;

    @NotNull
    private List<DatabaseAccess> accesses;

    @NotNull
    private List<Table> tables;

    @NotNull
    private List<View> views;

    @NotNull
    private List<Subset> subsets;

    @TimeToLive
    private Long exp;

}
