package at.tuwien.api.container;

import io.swagger.v3.oas.annotations.Parameter;
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
public class ContainerChangeDto {

    @NotNull
    @Parameter(required = true, example = "start")
    private ContainerActionTypeDto action;

}
