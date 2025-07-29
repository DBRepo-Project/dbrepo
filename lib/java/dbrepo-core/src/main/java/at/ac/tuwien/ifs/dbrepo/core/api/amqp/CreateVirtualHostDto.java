package at.ac.tuwien.ifs.dbrepo.core.api.amqp;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.extern.jackson.Jacksonized;

@Getter
@Setter
@Builder
@EqualsAndHashCode
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
public class CreateVirtualHostDto {

    @NotNull
    @Schema(description = "The name of the virtual host", example = "dbrepo")
    private String name;

    @Schema(description = "The description of the virtual host", example = "QA environment for issue 1662")
    private String description;

    @Schema(description = "The tags of the virtual host", example = "qa,project-a,qa-1662")
    private String tags;

}
