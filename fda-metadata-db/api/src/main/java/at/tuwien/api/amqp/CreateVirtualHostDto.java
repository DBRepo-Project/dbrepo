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
public class CreateVirtualHostDto {

    @NotNull
    @Parameter(name = "virtual host name")
    private String name;

    @Parameter(name = "virtual host description")
    private String description;

    @Parameter(name = "virtual host tags")
    private String tags;

}
