package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
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
@RedisHash("datatype")
public class DataType {

    @Id
    private UUID id;

    @NotNull
    private String value;

    private Integer dDefault;

    private Integer sizeDefault;

    @TimeToLive
    private Long exp;

}
