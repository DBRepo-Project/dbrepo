
package at.tuwien.api.document.file;

import at.tuwien.api.document.links.LinksDto;
import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;
import java.time.Instant;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileEntryDto {

    @NotBlank
    @Parameter(name = "file name", description = "Name of the file.")
    private String key;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC+2")
    @Parameter(name = "file updated")
    private Instant updated;

    @NotNull
    @JsonFormat(pattern = "yyyy-MM-dd'T'HH:mm:ss.SSSSSSXXX", timezone = "UTC+2")
    @Parameter(name = "file created")
    private Instant created;

    @Parameter(name = "file status")
    private String status;

    @Parameter(name = "file links")
    private LinksDto links;

}
