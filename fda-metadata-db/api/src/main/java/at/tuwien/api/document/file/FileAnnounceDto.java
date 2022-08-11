
package at.tuwien.api.document.file;

import at.tuwien.api.document.links.LinksDto;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotNull;
import java.util.List;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileAnnounceDto {

    @JsonProperty("default_preview")
    @Parameter(name = "file name")
    private String defaultPreview;

    @NotNull
    @Parameter(name = "file enabled")
    private Boolean enabled;

    @NotNull
    @Parameter(name = "file entries")
    private List<FileEntryDto> entries;

    @Parameter(name = "file links")
    private LinksDto links;

}
