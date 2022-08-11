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
public class TupleDto {

    @NotNull
    @Parameter(name = "key", example = "name")
    private String k;

    @NotNull
    @Parameter(name = "value", example = "Max Mustermann")
    private String v;

}
