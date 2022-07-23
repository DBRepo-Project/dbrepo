package at.tuwien.api.database.table;

import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.Parameter;
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
    @Parameter(name = "table id", example = "1")
    private Long id;

    @NotBlank(message = "name is required")
    @Parameter(name = "table name", example = "Weather Australia")
    private String name;

    @NotBlank(message = "internal name is required")
    @JsonProperty("internal_name")
    @Parameter(name = "table internal name", example = "weather_australia")
    private String internalName;

    @NotNull(message = "creator is required")
    @Parameter(name = "table creator")
    private UserBriefDto creator;

}
