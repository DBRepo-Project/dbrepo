package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

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
@RedisHash("table")
public class Table {

    @Id
    private UUID id;

    @jakarta.persistence.Column(nullable = false)
    private String internalName;

    @jakarta.persistence.Column(nullable = false)
    private Boolean isPublic;

    @jakarta.persistence.Column(nullable = false)
    private Boolean isSchemaPublic;

    private Long numRows;

    private Long dataLength;

    private Long maxDataLength;

    private Long avgRowLength;

    private String ownedBy;

    @NotNull
    private List<Column> columns;

    @TimeToLive
    private Long exp;

}
