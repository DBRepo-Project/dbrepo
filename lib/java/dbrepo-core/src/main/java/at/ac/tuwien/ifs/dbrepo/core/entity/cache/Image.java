package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.validation.constraints.NotBlank;
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
@RedisHash("image")
public class Image {

    @Id
    private UUID id;

    @NotBlank
    private String jdbcMethod;

    @NotNull
    private List<DataType> dataTypes;

    @NotNull
    private List<Operator> operators;

    @TimeToLive
    private Long exp;

}
