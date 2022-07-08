package at.tuwien.api.container;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@ToString
public class ContainerChangeDto {

    @NotNull
    @Parameter(required = true, example = "start")
    private ContainerActionTypeDto action;

}
