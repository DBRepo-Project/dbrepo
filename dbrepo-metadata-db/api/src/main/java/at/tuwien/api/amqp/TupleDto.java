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
public class TupleDto {

    @NotNull
    @Schema(example = "name")
    private String k;

    @NotNull
    @Schema(example = "Max Mustermann")
    private String v;

}
