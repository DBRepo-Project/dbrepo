package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;

import java.util.UUID;


@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@RedisHash("column")
public class Column {

    @Id
    private UUID id;

    @NotBlank
    private String internalName;

    @NotNull
    private ColumnType columnType;

    @TimeToLive
    private Long exp;

}
