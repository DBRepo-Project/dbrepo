package at.tuwien.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TupleDto {

    @NotNull
    @Schema(example = "name")
    private String k;

    @NotNull
    @Schema(example = "Max Mustermann")
    private String v;

}
