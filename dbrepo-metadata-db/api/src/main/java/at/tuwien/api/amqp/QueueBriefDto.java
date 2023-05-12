package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class QueueBriefDto {

    @NotNull
    @Schema(example = "dbrepo")
    private String vhost;

    @NotNull
    @Schema(example = "air")
    private String name;

}
