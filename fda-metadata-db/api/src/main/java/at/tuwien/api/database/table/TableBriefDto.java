package at.tuwien.api.database.table;

import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import javax.validation.constraints.NotBlank;
import javax.validation.constraints.NotNull;

@Getter
@Setter
@ToString
@Builder
@AllArgsConstructor
@NoArgsConstructor
public class TableBriefDto {

    @NotNull(message = "id is required")
    private Long id;

    @NotBlank(message = "name is required")
    @Schema(example = "Air Quality")
    private String name;

    @NotBlank(message = "description is required")
    @Schema(example = "Air Quality in Austria")
    private String description;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Schema(example = "air_quality")
    private String internalName;

    @NotNull(message = "creator is required")
    private UserBriefDto creator;

}
