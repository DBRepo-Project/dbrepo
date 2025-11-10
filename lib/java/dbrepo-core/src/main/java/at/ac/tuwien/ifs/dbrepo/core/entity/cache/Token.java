package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import lombok.*;
import org.springframework.data.annotation.Id;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;


@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@RedisHash("token")
public class Token {

    @Id
    private String username;

    private String token;

    @TimeToLive
    private Long exp;

}
