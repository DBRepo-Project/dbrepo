package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.persistence.Column;
import jakarta.persistence.Id;
import lombok.*;
import org.springframework.data.redis.core.RedisHash;
import org.springframework.data.redis.core.TimeToLive;


@Data
@Builder
@ToString
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@RedisHash("access")
public class DatabaseAccess {

    @Id
    private String username;

    @Column(nullable = false)
    private AccessType type;

    @TimeToLive
    private Long exp;

}
