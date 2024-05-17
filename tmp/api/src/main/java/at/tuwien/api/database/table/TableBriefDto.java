package at.tuwien.api.database.table;

import at.tuwien.api.database.table.columns.ColumnBriefDto;
import at.tuwien.api.user.UserBriefDto;
import com.fasterxml.jackson.annotation.JsonProperty;
import io.swagger.v3.oas.annotations.media.Schema;
import lombok.*;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.extern.jackson.Jacksonized;

import java.util.List;

@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Jacksonized
@ToString
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

    @NotNull
    @JsonProperty("is_versioned")
    @Schema(example = "true")
    private Boolean isVersioned;

    @NotNull(message = "owner is required")
    private UserBriefDto owner;

    @NotNull(message = "columns are required")
    private List<ColumnBriefDto> columns;
}
