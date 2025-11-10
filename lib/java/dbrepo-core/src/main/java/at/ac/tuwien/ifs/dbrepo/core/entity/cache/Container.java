package at.ac.tuwien.ifs.dbrepo.core.entity.cache;

import jakarta.persistence.Column;
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
@RedisHash("container")
public class Container {

    @Id
    private UUID id;

    @Column(nullable = false)
    private String internalName;

    @Column(nullable = false)
    private String host;

    @Column(nullable = false)
    private String username;

    @Column(nullable = false)
    private String password;

    @Column(nullable = false)
    private Integer port;

    @NotNull
    private Image image;

    @TimeToLive
    private Long exp;

}
