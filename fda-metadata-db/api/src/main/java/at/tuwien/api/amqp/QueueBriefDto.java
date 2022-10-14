package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Schema;
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
    @Schema(example = "%2F")
    private String vhost;

    @NotNull
    @Schema(example = "air")
    private String name;

}
