package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class QueueBriefDto {

    @NotNull
    @Parameter(name = "queue vhost")
    private String vhost;

    @NotNull
    @Parameter(name = "queue name")
    private String name;

}
