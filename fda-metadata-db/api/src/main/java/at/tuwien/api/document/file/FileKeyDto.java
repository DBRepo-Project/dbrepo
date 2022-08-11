
package at.tuwien.api.document.file;

import com.fasterxml.jackson.annotation.JsonInclude;
import io.swagger.v3.oas.annotations.Parameter;
import lombok.*;

import javax.validation.constraints.NotBlank;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class FileKeyDto {

    @NotBlank
    @Parameter(name = "file name", description = "Name of the file.", example = "mock.png")
    private String key;

}
