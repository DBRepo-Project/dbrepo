package at.tuwien.api.document.record;

import at.tuwien.api.document.metadata.MetadataDto;
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
public class CreateDraftDto {

    @NotNull(message = "access is required")
    @Parameter(name = "access")
    private AccessOptionsDto access;

    @NotNull(message = "files options is required")
    @Parameter(name = "files options")
    private FilesOptionsDto files;

    @NotNull(message = "metadata is required")
    @Parameter(name = "metadata")
    private MetadataDto metadata;

}
