package at.tuwien.api.document.file;

import at.tuwien.api.document.links.LinksDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileStartDto {

    @JsonProperty("default_preview")
    @Parameter(name = "default preview", description = "Filename of file to be previewed by default.")
    private String defaultPreview;

    @NotNull(message = "enabled is required")
    @Parameter(name = "enabled", description = "Should (and can) files be attached to this record or not.")
    private Boolean enabled;

    @NotNull(message = "entries is required")
    @Parameter(name = "entries")
    private List<EntryDto> entries;

    @NotNull(message = "links is required")
    @Parameter(name = "links")
    private LinksDto links;

    @Parameter(name = "order", description = "Array of filename strings in display order.")
    private List<String> order;

}
