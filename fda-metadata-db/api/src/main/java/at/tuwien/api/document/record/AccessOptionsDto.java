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
public class AccessOptionsDto {

    @NotNull(message = "record access type is required")
    @Parameter(name = "record access type")
    private AccessTypeDto record;

    @NotNull(message = "files is required")
    @Parameter(name = "files")
    private FileTypeDto files;

    @Parameter(name = "embargo options")
    private EmbargoDto embargo;

}
