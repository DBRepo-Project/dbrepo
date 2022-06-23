package at.tuwien.api.document.record;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FilesOptionsDto {

    @NotNull(message = "enabled is required")
    @Parameter(name = "files enabled", description = "Should (and can) files be attached to this record or not.")
    private Boolean enabled;

    @Parameter(name = "files preview", description = "Filename of file to be previewed by default.")
    private String default_preview;
}
