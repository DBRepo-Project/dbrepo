package at.tuwien.api.database.query;

import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@Builder
@AllArgsConstructor
@NoArgsConstructor
@EqualsAndHashCode
@ToString
public class ImportDto {

    @NotBlank(message = "location is required")
    @Parameter(name = "csv location")
    private String location;
}
