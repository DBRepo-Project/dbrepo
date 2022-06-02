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
public class GrantComponentDto {

    @NotNull
    @Parameter(name = "component name")
    private String name;

    @NotNull
    @Parameter(name = "username")
    private String username;

}
