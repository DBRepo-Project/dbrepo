package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.validation.constraints.NotBlank;
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
@RedisHash("operator")
public class Operator {

    @Id
    private UUID id;

    @NotBlank
    private String value;

    @TimeToLive
    private Long exp;

}
