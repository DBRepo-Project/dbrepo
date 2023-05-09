package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class CreateVirtualHostDto {

    @NotNull
    @Schema(example = "air")
    private String name;

    private String description;

    private String tags;

}
