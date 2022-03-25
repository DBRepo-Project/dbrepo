package at.tuwien.api.database;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class DatabaseCreateDto {

    @NotBlank(message = "database name is required")
    @Parameter(name = "database name", example = "Weather Australia")
    private String name;

    @NotBlank(message = "description is required")
    @Parameter(name = "database description", example = "true")
    private String description;

}
